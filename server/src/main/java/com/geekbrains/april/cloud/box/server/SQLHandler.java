package com.geekbrains.april.cloud.box.server;

import com.geekbrains.april.cloud.box.common.FileInfo;

import java.sql.*;
import java.util.ArrayList;

public class SQLHandler {
    private static Connection connection;
    private static PreparedStatement psAuthorization;
    private static PreparedStatement psGetFilesList;
    private static PreparedStatement psGetFileInfo;
    private static PreparedStatement psFileExists;
    private static PreparedStatement psInsertFile;
    private static PreparedStatement psUpdateFile;
    private static PreparedStatement psDeleteFile;
    private static PreparedStatement psUserFiles;
    private static PreparedStatement psInsertWorkingFile;
    private static PreparedStatement psDeleteWorkingFile;
    private static PreparedStatement psSelectWorkingFileWithoutUserId;


    public static boolean connect(String url, String user, String password, String driver) {
        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(url, user, password);
            psAuthorization = connection.prepareStatement("SELECT id FROM users WHERE login = ? AND password = ?;");
            psGetFilesList = connection.prepareStatement("SELECT fileName, md5, fullLength, currentLength FROM files_of_user_view WHERE user_id = ?;");
            psGetFileInfo = connection.prepareStatement("SELECT md5, fullLength, currentLength FROM files_of_user_view WHERE user_id = ? AND fileName = ?;");
            psFileExists = connection.prepareStatement("SELECT id FROM files WHERE md5 = ?;");
            psInsertFile = connection.prepareStatement("INSERT INTO files (md5, length, position) VALUES (?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
            psUpdateFile = connection.prepareStatement("UPDATE files SET md5 = ?, length = ?, position = ? WHERE id = ?;");
            psDeleteFile = connection.prepareStatement("DELETE FROM files WHERE id = ?;");
            psUserFiles = connection.prepareStatement("SELECT id, filename FROM user_files WHERE user_id = ? AND file_id = ?;");
            psInsertWorkingFile = connection.prepareStatement("INSERT INTO user_files (user_id, file_id, filename) VALUES (?, ?, ?);");
            psDeleteWorkingFile = connection.prepareStatement("DELETE FROM user_files WHERE id = ?;");
            psSelectWorkingFileWithoutUserId = connection.prepareStatement("SELECT id FROM user_files WHERE file_id = ?;");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void disconnect() {
        try {
            psAuthorization.close();
            psGetFilesList.close();
            psFileExists.close();
            psInsertFile.close();
            psUpdateFile.close();
            psDeleteFile.close();
            psUserFiles.close();
            psInsertWorkingFile.close();
            psDeleteWorkingFile.close();
            psSelectWorkingFileWithoutUserId.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int authorize(String login, String password) {
        try {
            psAuthorization.setString(1, login);
            psAuthorization.setString(2, password);
            ResultSet rs = psAuthorization.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static ArrayList<FileInfo> getUserFilesList(int user_id) {
        ArrayList<FileInfo> fileInfoArrayList = new ArrayList<>();
        try {
            psGetFilesList.setInt(1, user_id);
            ResultSet rs = psGetFilesList.executeQuery();
            while (rs.next()) {
                FileInfo fi = new FileInfo();
                fi.fileName = rs.getString("fileName");
                fi.MD5 = rs.getString("md5");
                fi.fileLength = rs.getLong("fullLength");
                fi.position = rs.getLong("currentLength");
                fileInfoArrayList.add(fi);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return fileInfoArrayList;
    }

    public static void insertOrUpdateWorkingFile(FileInfo fileInfo, int user_id) {
        try {
            connection.setAutoCommit(false);

            int file_id = 0;
            psFileExists.setString(1, fileInfo.MD5);
            ResultSet rs = psFileExists.executeQuery();
            if (rs.next()) file_id = rs.getInt("id");
            rs.close();

            if (file_id == 0) {
                psInsertFile.setString(1, fileInfo.MD5);
                psInsertFile.setLong(2, fileInfo.fileLength);
                psInsertFile.setLong(3, fileInfo.position);
                psInsertFile.executeUpdate();
                ResultSet rsi = psInsertFile.getGeneratedKeys();
                if (rsi.next()) file_id = rsi.getInt(1);
                rsi.close();
            } else {
                psUpdateFile.setString(1, fileInfo.MD5);
                psUpdateFile.setLong(2, fileInfo.fileLength);
                psUpdateFile.setLong(3, fileInfo.position);
                psUpdateFile.setInt(4, file_id);
                psUpdateFile.executeUpdate();
            }

            psUserFiles.setInt(1, user_id);
            psUserFiles.setInt(2, file_id);
            ResultSet rsuf = psUserFiles.executeQuery();
            boolean isFindName = false;
            while (rsuf.next()) {
                if (rsuf.getString("filename").equals(fileInfo.fileName)) {
                    isFindName = true;
                    break;
                }
            }
            rsuf.close();
            if (!isFindName) {
                psInsertWorkingFile.setInt(1, user_id);
                psInsertWorkingFile.setInt(2, file_id);
                psInsertWorkingFile.setString(3, fileInfo.fileName);
                psInsertWorkingFile.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            if (connection != null) {
                try {
                    System.err.print("Transaction is being rolled back");
                    connection.rollback();
                } catch (SQLException excep) {
                    e.printStackTrace();
                }
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean deleteWorkingFile(FileInfo fileInfo, int user_id) {
        boolean isLastReference = false;
        try {
            connection.setAutoCommit(false);

            int file_id = 0;
            psFileExists.setString(1, fileInfo.MD5);
            ResultSet rs = psFileExists.executeQuery();
            if (rs.next()) file_id = rs.getInt("id");
            rs.close();

            if (file_id != 0) {
                psUserFiles.setInt(1, user_id);
                psUserFiles.setInt(2, file_id);
                ResultSet rsuf = psUserFiles.executeQuery();
                int id = 0;
                while (rsuf.next()) {
                    if (rsuf.getString("filename").equals(fileInfo.fileName)) {
                        id = rsuf.getInt("id");
                        break;
                    }
                }
                rsuf.close();
                if (id != 0) {
                    psDeleteWorkingFile.setInt(1, id);
                    psDeleteWorkingFile.executeUpdate();
                    psSelectWorkingFileWithoutUserId.setInt(1, file_id);
                    ResultSet rswu = psSelectWorkingFileWithoutUserId.executeQuery();
                    if (!rswu.next()) {
                        psDeleteFile.setInt(1, file_id);
                        psDeleteFile.executeUpdate();
                        isLastReference = true;
                    }
                    rswu.close();
                }
            }

            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            if (connection != null) {
                try {
                    System.err.print("Transaction is being rolled back");
                    connection.rollback();
                } catch (SQLException excep) {
                    e.printStackTrace();
                }
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return isLastReference;
    }

    public static FileInfo getFileInfoDB(FileInfo fileInfo, int user_id) throws CloneNotSupportedException {
        FileInfo fileInfoDB = null;
        try {
            psGetFileInfo.setInt(1, user_id);
            psGetFileInfo.setString(2, fileInfo.fileName);
            ResultSet rs = psGetFileInfo.executeQuery();
            if (rs.next()) {
                fileInfoDB = (FileInfo) fileInfo.clone();
                fileInfoDB.MD5 = rs.getString("md5");
                fileInfoDB.position = rs.getLong("currentLength");
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return fileInfoDB;
    }

}
