package com.bridgeinspection.service;

import com.bridgeinspection.common.BusinessException;
import com.bridgeinspection.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class VersionControlService {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final DateTimeFormatter MESSAGE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final Path repositoryRoot;

    public VersionControlService(JdbcTemplate jdbcTemplate,
                                 @Value("${app.version.repository-root:..}") String repositoryRoot) {
        this.jdbcTemplate = jdbcTemplate;
        this.repositoryRoot = Path.of(repositoryRoot).toAbsolutePath().normalize();
    }

    public Map<String, Object> summary() {
        boolean initialized = isInitialized();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("initialized", initialized);
        result.put("repository_path", repositoryRoot.toString());
        result.put("branch", initialized ? branch() : "未初始化");
        result.put("dirty_count", initialized ? changedFiles().size() : 0);
        result.put("changed_files", initialized ? changedFiles() : List.of());
        result.put("latest_commit", initialized ? latestCommit() : null);
        result.put("commits", initialized ? commits() : List.of());
        result.put("remote_url", initialized ? remoteUrl() : "");
        result.put("current_version", currentVersion());
        result.put("system_versions", systemVersions());
        result.put("backups", backups());
        return result;
    }

    public synchronized Map<String, Object> checkGithubUpdates() {
        initialize();
        String remote = remoteUrl();
        if (remote.isBlank()) throw new BusinessException("尚未配置 GitHub 远端仓库");
        String installedVersion = currentVersion();
        String installedCommit = currentVersionCommit();
        if (installedCommit.isBlank()) {
            installedCommit = runGit(false, "rev-parse", "HEAD").output().trim();
            jdbcTemplate.update("UPDATE tb_system_version SET git_commit=?,build_time=COALESCE(build_time,NOW()),repository_url=? WHERE id=(SELECT id FROM (SELECT id FROM tb_system_version ORDER BY id DESC LIMIT 1) current_version)",
                    installedCommit, canonicalRepositoryUrl(remote));
        }
        runGit(false, "fetch", "origin", "--prune");
        String currentBranch = branch();
        GitResult remoteHead = runGit(true, "rev-parse", "--verify", "origin/" + currentBranch);
        if (remoteHead.exitCode() != 0) throw new BusinessException("GitHub 上不存在分支 " + currentBranch);
        String remoteCommit = remoteHead.output().trim();
        int ahead = revisionCount("origin/" + currentBranch + "..HEAD");
        int behind = revisionCount("HEAD..origin/" + currentBranch);
        GitResult latest = runGit(true, "log", "-1", "--pretty=format:%h%x1f%s%x1f%aI", "origin/" + currentBranch);
        String[] fields = latest.output().split("\\u001f", -1);
        boolean updateAvailable = !remoteCommit.equals(installedCommit);
        String availableVersion = updateAvailable ? nextMinorVersion(installedVersion) : installedVersion;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("remote_url", canonicalRepositoryUrl(remote));
        result.put("branch", currentBranch);
        result.put("ahead", ahead);
        result.put("behind", behind);
        result.put("current_version", installedVersion);
        result.put("available_version", availableVersion);
        result.put("update_available", updateAvailable);
        result.put("remote_hash", remoteCommit);
        result.put("remote_commit", fields.length > 0 ? fields[0] : "");
        result.put("remote_message", fields.length > 1 ? fields[1] : "");
        result.put("remote_time", fields.length > 2 ? fields[2] : "");
        result.put("message", updateAvailable ? "发现 " + availableVersion + " 更新" : "当前已是最新版本 " + installedVersion);
        return result;
    }

    @Transactional
    public synchronized Map<String, Object> applyGithubUpdate() {
        Map<String, Object> update = checkGithubUpdates();
        if (!Boolean.TRUE.equals(update.get("update_available"))) return update;
        assertCleanRepository();
        String targetVersion = String.valueOf(update.get("available_version"));
        String targetCommit = String.valueOf(update.get("remote_hash"));
        String safetyBranch = createSafetyBranch("update-safety");
        Map<String, Object> backup = performDatabaseBackup("更新到 " + targetVersion + " 前自动备份");
        runGit(false, "reset", "--hard", targetCommit);
        registerSystemVersion(targetVersion, targetCommit, String.valueOf(update.get("remote_url")));
        Map<String, Object> result = new LinkedHashMap<>(update);
        result.put("current_version", targetVersion);
        result.put("update_available", false);
        result.put("safety_branch", safetyBranch);
        result.put("backup_id", backup.get("backup_id"));
        result.put("message", "已更新到 " + targetVersion + "，请重启系统使新版本生效");
        return result;
    }

    @Transactional
    public synchronized Map<String, Object> rollbackVersion(String requestedVersion) {
        String version = normalizeVersion(requestedVersion);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT version_no,git_commit,repository_url FROM tb_system_version
                WHERE version_no=? AND git_commit IS NOT NULL AND git_commit<>''
                ORDER BY id DESC LIMIT 1
                """, version);
        if (rows.isEmpty()) throw new BusinessException("没有找到 " + version + " 的可回溯代码版本");
        if (version.equals(currentVersion())) throw new BusinessException("当前已经是 " + version);
        String targetCommit = String.valueOf(rows.get(0).get("git_commit"));
        GitResult commit = runGit(true, "cat-file", "-e", targetCommit + "^{commit}");
        if (commit.exitCode() != 0) throw new BusinessException(version + " 对应的 Git 提交不存在，无法回溯");
        assertCleanRepository();
        String safetyBranch = createSafetyBranch("rollback-safety");
        Map<String, Object> backup = performDatabaseBackup("回溯到 " + version + " 前自动备份");
        runGit(false, "reset", "--hard", targetCommit);
        registerSystemVersion(version, targetCommit,
                canonicalRepositoryUrl(String.valueOf(rows.get(0).get("repository_url"))));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("current_version", version);
        result.put("git_commit", targetCommit);
        result.put("safety_branch", safetyBranch);
        result.put("backup_id", backup.get("backup_id"));
        result.put("message", "已回溯到 " + version + "，请重启系统使历史版本生效");
        return result;
    }

    public synchronized Map<String, Object> initialize() {
        try {
            Files.createDirectories(repositoryRoot);
        } catch (IOException ex) {
            throw new BusinessException("无法创建版本仓库目录：" + ex.getMessage());
        }
        if (!isInitialized()) runGit(false, "init", "--initial-branch=main");
        runGit(false, "config", "user.name", "Bridge Inspection System");
        runGit(false, "config", "user.email", "bridge-inspection@local");
        return summary();
    }

    @Transactional
    public synchronized Map<String, Object> createVersion(String requestedMessage) {
        initialize();
        String message = requestedMessage == null || requestedMessage.isBlank()
                ? "系统版本 " + LocalDateTime.now().format(MESSAGE_TIME)
                : requestedMessage.trim();
        if (message.length() > 300) throw new BusinessException("版本说明不能超过300个字符");

        Map<String, Object> backup = performDatabaseBackup(message);
        long backupId = ((Number) backup.get("backup_id")).longValue();
        try {
            runGit(false, "add", "--all", "--", ".");
            runGit(false, "commit", "-m", message);
            String commitHash = runGit(false, "rev-parse", "HEAD").output().trim();
            String currentBranch = branch();
            jdbcTemplate.update("""
                    UPDATE tb_backup_record SET git_commit_hash=?,git_branch=?,backup_status='成功'
                    WHERE backup_id=?
                    """, commitHash, currentBranch, backupId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("backup_id", backupId);
            result.put("commit_hash", commitHash);
            result.put("short_hash", commitHash.substring(0, Math.min(8, commitHash.length())));
            result.put("branch", currentBranch);
            result.put("message", message);
            return result;
        } catch (RuntimeException ex) {
            jdbcTemplate.update("UPDATE tb_backup_record SET backup_status='版本提交失败',error_message=? WHERE backup_id=?",
                    abbreviate(ex.getMessage(), 1000), backupId);
            throw ex;
        }
    }

    public Resource backupFile(long backupId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT file_path FROM tb_backup_record WHERE backup_id=? AND backup_status='成功'", backupId);
        if (rows.isEmpty()) throw new BusinessException("数据库备份不存在或尚未成功完成");
        Path file = repositoryRoot.resolve(String.valueOf(rows.get(0).get("file_path"))).normalize();
        Path backupRoot = repositoryRoot.resolve("backup").resolve("database").normalize();
        if (!file.startsWith(backupRoot) || !Files.isRegularFile(file)) throw new BusinessException("数据库备份文件不存在");
        return new PathResource(file);
    }

    @Transactional
    public synchronized Map<String, Object> deleteBackup(long backupId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT file_name,file_path FROM tb_backup_record WHERE backup_id=?", backupId);
        if (rows.isEmpty()) throw new BusinessException("数据库备份记录不存在");
        Map<String, Object> row = rows.get(0);
        Path backupRoot = repositoryRoot.resolve("backup").resolve("database").normalize();
        Path file = repositoryRoot.resolve(String.valueOf(row.get("file_path"))).normalize();
        if (!file.startsWith(backupRoot)) throw new BusinessException("备份文件路径不合法，拒绝删除");
        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            throw new BusinessException("删除数据库备份文件失败：" + ex.getMessage());
        }
        jdbcTemplate.update("DELETE FROM tb_backup_record WHERE backup_id=?", backupId);
        return Map.of("backup_id", backupId, "file_name", String.valueOf(row.get("file_name")));
    }

    @Transactional
    public synchronized Map<String, Object> createDatabaseBackup(String requestedMessage) {
        String message = requestedMessage == null || requestedMessage.isBlank()
                ? "数据库备份 " + LocalDateTime.now().format(MESSAGE_TIME)
                : requestedMessage.trim();
        if (message.length() > 300) throw new BusinessException("备份说明不能超过300个字符");
        return performDatabaseBackup(message);
    }

    private Map<String, Object> performDatabaseBackup(String versionMessage) {
        Path backupDirectory = repositoryRoot.resolve("backup").resolve("database");
        String fileName = "bridge_inspection_" + LocalDateTime.now().format(FILE_TIME) + ".sql";
        Path file = backupDirectory.resolve(fileName).normalize();
        try {
            Files.createDirectories(backupDirectory);
            writeDump(file);
            long size = Files.size(file);
            String sha256 = sha256(file);
            String relativePath = repositoryRoot.relativize(file).toString().replace('\\', '/');
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                var statement = connection.prepareStatement("""
                        INSERT INTO tb_backup_record
                            (backup_type,version_message,file_name,file_path,file_size,sha256,execute_by,backup_status)
                        VALUES ('database',?,?,?,?,?,?, '成功')
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, versionMessage);
                statement.setString(2, fileName);
                statement.setString(3, relativePath);
                statement.setLong(4, size);
                statement.setString(5, sha256);
                statement.setInt(6, currentUserId());
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) throw new BusinessException("数据库备份记录写入失败");
            Map<String, Object> backup = new LinkedHashMap<>();
            backup.put("backup_id", key.longValue());
            backup.put("file_name", fileName);
            backup.put("file_path", relativePath);
            backup.put("file_size", size);
            backup.put("sha256", sha256);
            return backup;
        } catch (IOException ex) {
            throw new BusinessException("数据库备份失败：" + ex.getMessage());
        }
    }

    private void writeDump(Path file) throws IOException {
        List<String> tables = jdbcTemplate.query(
                "SHOW FULL TABLES WHERE Table_type='BASE TABLE'", (rs, rowNum) -> rs.getString(1));
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            writer.write("-- Bridge Inspection System database backup\n");
            writer.write("-- Generated at " + LocalDateTime.now().format(MESSAGE_TIME) + "\n\n");
            writer.write("SET NAMES utf8mb4;\nSET FOREIGN_KEY_CHECKS=0;\n\n");
            for (String table : tables) {
                Map<String, Object> createRow = jdbcTemplate.queryForMap("SHOW CREATE TABLE `" + quoteName(table) + "`");
                Object[] createValues = createRow.values().toArray();
                String createSql = String.valueOf(createValues[createValues.length - 1]);
                writer.write("DROP TABLE IF EXISTS `" + quoteName(table) + "`;\n");
                writer.write(createSql + ";\n");
                try {
                    jdbcTemplate.query("SELECT * FROM `" + quoteName(table) + "`", rs -> {
                        try {
                            ResultSetMetaData meta = rs.getMetaData();
                            int columnCount = meta.getColumnCount();
                            while (rs.next()) {
                                writer.write("INSERT INTO `" + quoteName(table) + "` (");
                                for (int column = 1; column <= columnCount; column++) {
                                    if (column > 1) writer.write(',');
                                    writer.write('`' + quoteName(meta.getColumnLabel(column)) + '`');
                                }
                                writer.write(") VALUES (");
                                for (int column = 1; column <= columnCount; column++) {
                                    if (column > 1) writer.write(',');
                                    writer.write(sqlValue(rs.getObject(column)));
                                }
                                writer.write(");\n");
                            }
                            return null;
                        } catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }
                    });
                } catch (UncheckedIOException ex) {
                    throw ex.getCause();
                }
                writer.write('\n');
            }
            writer.write("SET FOREIGN_KEY_CHECKS=1;\n");
        }
    }

    private String sqlValue(Object value) {
        if (value == null) return "NULL";
        if (value instanceof Number) return value.toString();
        if (value instanceof Boolean bool) return bool ? "1" : "0";
        if (value instanceof byte[] bytes) return "X'" + HexFormat.of().formatHex(bytes) + "'";
        String text = String.valueOf(value)
                .replace("\\", "\\\\")
                .replace("\u0000", "\\0")
                .replace("'", "\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        return "'" + text + "'";
    }

    private String currentVersion() {
        List<String> versions = jdbcTemplate.query(
                "SELECT version_no FROM tb_system_version ORDER BY id DESC LIMIT 1",
                (rs, rowNum) -> rs.getString(1));
        return versions.isEmpty() ? "V1.0" : normalizeVersion(versions.get(0));
    }

    private String currentVersionCommit() {
        List<String> commits = jdbcTemplate.query(
                "SELECT COALESCE(git_commit,'') FROM tb_system_version ORDER BY id DESC LIMIT 1",
                (rs, rowNum) -> rs.getString(1));
        return commits.isEmpty() || commits.get(0) == null ? "" : commits.get(0);
    }

    private List<Map<String, Object>> systemVersions() {
        List<Map<String, Object>> versions = jdbcTemplate.queryForList("""
                SELECT id,version_no,git_commit,build_time,repository_url,create_time
                FROM tb_system_version ORDER BY id DESC LIMIT 20
                """);
        versions.forEach(row -> row.put("version_no", normalizeVersion(String.valueOf(row.get("version_no")))));
        return versions;
    }

    private String normalizeVersion(String value) {
        String version = value == null ? "" : value.trim().toUpperCase();
        if (version.startsWith("V")) version = version.substring(1);
        String[] parts = version.split("\\.");
        String major = parts.length > 0 && parts[0].matches("\\d+") ? parts[0] : "1";
        String minor = parts.length > 1 && parts[1].matches("\\d+") ? parts[1] : "0";
        return "V" + major + "." + minor;
    }

    private String nextMinorVersion(String value) {
        String[] parts = normalizeVersion(value).substring(1).split("\\.");
        return "V" + parts[0] + "." + (Integer.parseInt(parts[1]) + 1);
    }

    private String canonicalRepositoryUrl(String value) {
        String remote = value == null || value.isBlank()
                ? "https://github.com/Xiaotu-666/Bridge"
                : value.trim();
        return remote.endsWith(".git") ? remote.substring(0, remote.length() - 4) : remote;
    }

    private void assertCleanRepository() {
        if (!changedFiles().isEmpty()) {
            throw new BusinessException("当前代码目录存在未保存修改，请先提交或备份代码后再执行版本更新或回溯");
        }
    }

    private String createSafetyBranch(String prefix) {
        String name = prefix + "-" + LocalDateTime.now().format(FILE_TIME);
        runGit(false, "branch", name, "HEAD");
        return name;
    }

    private void registerSystemVersion(String version, String commit, String repositoryUrl) {
        jdbcTemplate.update("""
                INSERT INTO tb_system_version(version_no,git_commit,build_time,repository_url)
                VALUES (?,?,NOW(),?)
                """, normalizeVersion(version), commit, canonicalRepositoryUrl(repositoryUrl));
    }

    private List<Map<String, Object>> backups() {
        return jdbcTemplate.queryForList("""
                SELECT b.backup_id,b.backup_type,b.version_message,b.file_name,b.file_path,b.file_size,b.sha256,
                       b.git_commit_hash,b.git_branch,b.backup_status,b.restore_status,b.error_message,
                       b.create_time,b.restore_time,u.user_name execute_name
                FROM tb_backup_record b LEFT JOIN tb_user u ON u.user_id=b.execute_by
                ORDER BY b.create_time DESC,b.backup_id DESC LIMIT 50
                """);
    }

    private List<Map<String, Object>> commits() {
        GitResult result = runGit(true, "log", "-20", "--pretty=format:%H%x1f%h%x1f%s%x1f%an%x1f%aI");
        if (result.exitCode() != 0 || result.output().isBlank()) return List.of();
        List<Map<String, Object>> commits = new ArrayList<>();
        for (String line : result.output().split("\\R")) {
            String[] fields = line.split("\\u001f", -1);
            if (fields.length < 5) continue;
            Map<String, Object> commit = new LinkedHashMap<>();
            commit.put("hash", fields[0]);
            commit.put("short_hash", fields[1]);
            commit.put("message", fields[2]);
            commit.put("author", fields[3]);
            commit.put("time", fields[4]);
            commits.add(commit);
        }
        return commits;
    }

    private Map<String, Object> latestCommit() {
        List<Map<String, Object>> commits = commits();
        return commits.isEmpty() ? null : commits.get(0);
    }

    private List<String> changedFiles() {
        GitResult result = runGit(true, "status", "--porcelain");
        if (result.exitCode() != 0 || result.output().isBlank()) return List.of();
        return result.output().lines().map(String::trim).filter(line -> !line.isBlank()).limit(100).toList();
    }

    private String branch() {
        GitResult result = runGit(true, "branch", "--show-current");
        String branch = result.output().trim();
        return branch.isBlank() ? "main" : branch;
    }

    private String remoteUrl() {
        GitResult result = runGit(true, "remote", "get-url", "origin");
        return result.exitCode() == 0 ? result.output().trim() : "";
    }

    private int revisionCount(String range) {
        GitResult result = runGit(true, "rev-list", "--count", range);
        try { return Integer.parseInt(result.output().trim()); }
        catch (Exception ex) { return 0; }
    }

    private boolean isInitialized() {
        return Files.isDirectory(repositoryRoot.resolve(".git"));
    }

    private GitResult runGit(boolean allowFailure, String... arguments) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-c");
        command.add("core.quotepath=false");
        command.add("-C");
        command.add(repositoryRoot.toString());
        command.addAll(List.of(arguments));
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new BusinessException("Git操作超时");
            }
            int exitCode = process.exitValue();
            if (!allowFailure && exitCode != 0) throw new BusinessException("Git操作失败：" + output.trim());
            return new GitResult(exitCode, output);
        } catch (IOException ex) {
            throw new BusinessException("无法执行Git，请确认已安装Git：" + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Git操作被中断");
        }
    }

    private String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String quoteName(String name) {
        return name.replace("`", "``");
    }

    private int currentUserId() {
        try {
            return Integer.parseInt(SecurityUtils.currentUserId());
        } catch (Exception ex) {
            throw new BusinessException(401, "请先登录");
        }
    }

    private String abbreviate(String value, int limit) {
        if (value == null) return null;
        return value.length() <= limit ? value : value.substring(0, limit);
    }

    private record GitResult(int exitCode, String output) {
    }
}
