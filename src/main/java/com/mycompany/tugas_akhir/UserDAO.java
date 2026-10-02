package com.mycompany.tugas_akhir;
<<<<<<< HEAD
// Obsolete legacy DAO - Replaced by UserRepository & UserService
=======

import java.sql.*;


public class UserDAO {

    
    public static class User {
        public int    id;
        public String fullName;
        public String displayName;       
        public String email;
        public String phone;             
        public String bio;               
        public String profilePicturePath; 
        public String role;

        public User(int id, String fullName, String displayName, String email,
                    String phone, String bio, String profilePicturePath, String role) {
            this.id                  = id;
            this.fullName            = fullName;
            this.displayName         = displayName;
            this.email               = email;
            this.phone               = phone;
            this.bio                 = bio;
            this.profilePicturePath  = profilePicturePath;
            this.role                = role;
        }

        
        public User(int id, String fullName, String email, String role) {
            this(id, fullName, null, email, null, null, null, role);
        }

        
        public String getEffectiveName() {
            return (displayName != null && !displayName.isBlank()) ? displayName : fullName;
        }

        
        public String getInitial() {
            String name = getEffectiveName();
            return (name != null && !name.isEmpty())
                    ? String.valueOf(name.charAt(0)).toUpperCase()
                    : "?";
        }
    }

    
    
    public boolean register(String fullName, String email, String plainPassword) {
        if (emailExists(email)) return false;

        String sql = "INSERT INTO users (full_name, email, password_hash, role) VALUES (?, ?, ?, 'user')";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, PasswordHelper.hashPassword(plainPassword));
            int affected = ps.executeUpdate();
            System.out.println("[UserDAO] Registrasi berhasil: " + email);
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Gagal registrasi: " + e.getMessage());
            return false;
        }
    }

    
    
    public User login(String emailOrUsername, String plainPassword) {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        if (conn == null) {
            System.err.println("[UserDAO] ✗ Koneksi database tidak tersedia!");
            return null;
        }

        String sql = "SELECT id, full_name, display_name, email, phone, bio, "
                   + "profile_picture_path, password_hash, role "
                   + "FROM users WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, emailOrUsername);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (PasswordHelper.verifyPassword(plainPassword, storedHash)) {
                    System.out.println("[UserDAO] ✓ Login berhasil: " + emailOrUsername);
                    return mapRow(rs);
                } else {
                    System.out.println("[UserDAO] ✗ Password salah untuk: " + emailOrUsername);
                }
            } else {
                System.out.println("[UserDAO] ✗ Email tidak ditemukan: " + emailOrUsername);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] ✗ Gagal login query: " + e.getMessage());
        }
        return null;
    }

    
    public User getUserById(int id) {
        String sql = "SELECT id, full_name, display_name, email, phone, bio, "
                   + "profile_picture_path, role FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRowNoPassword(rs);
        } catch (SQLException e) {
            System.err.println("[UserDAO] getUserById: " + e.getMessage());
        }
        return null;
    }

    
    
    public boolean updateProfile(int id, String fullName, String displayName,
                                 String phone, String bio) {
        String sql = "UPDATE users SET full_name=?, display_name=?, phone=?, bio=?, "
                   + "updated_at=CURRENT_TIMESTAMP WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, displayName == null || displayName.isBlank() ? null : displayName);
            ps.setString(3, phone == null || phone.isBlank() ? null : phone);
            ps.setString(4, bio == null || bio.isBlank() ? null : bio);
            ps.setInt(5, id);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) System.out.println("[UserDAO] Profil diupdate untuk id=" + id);
            return ok;
        } catch (SQLException e) {
            System.err.println("[UserDAO] updateProfile: " + e.getMessage());
            return false;
        }
    }

    
    public boolean updateProfilePicture(int id, String picturePath) {
        String sql = "UPDATE users SET profile_picture_path=?, updated_at=CURRENT_TIMESTAMP WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, picturePath);
            ps.setInt(2, id);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) System.out.println("[UserDAO] Foto profil diupdate untuk id=" + id);
            return ok;
        } catch (SQLException e) {
            System.err.println("[UserDAO] updateProfilePicture: " + e.getMessage());
            return false;
        }
    }

    
    
    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        
        String sqlGet = "SELECT password_hash FROM users WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement psGet = conn.prepareStatement(sqlGet)) {
            psGet.setInt(1, userId);
            ResultSet rs = psGet.executeQuery();
            if (!rs.next()) return false;

            String currentHash = rs.getString("password_hash");
            if (!PasswordHelper.verifyPassword(oldPassword, currentHash)) {
                System.out.println("[UserDAO] Ganti password gagal: password lama salah");
                return false;
            }

            
            String newHash = PasswordHelper.hashPassword(newPassword);
            String sqlUp = "UPDATE users SET password_hash=?, updated_at=CURRENT_TIMESTAMP WHERE id=?";
            try (PreparedStatement psUp = conn.prepareStatement(sqlUp)) {
                psUp.setString(1, newHash);
                psUp.setInt(2, userId);
                boolean ok = psUp.executeUpdate() > 0;
                if (ok) System.out.println("[UserDAO] Password berhasil diubah untuk id=" + userId);
                return ok;
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] changePassword: " + e.getMessage());
            return false;
        }
    }

    
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Gagal cek email: " + e.getMessage());
        }
        return false;
    }

    
    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("full_name"),
            rs.getString("display_name"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getString("bio"),
            rs.getString("profile_picture_path"),
            rs.getString("role")
        );
    }

    
    private User mapRowNoPassword(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("full_name"),
            rs.getString("display_name"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getString("bio"),
            rs.getString("profile_picture_path"),
            rs.getString("role")
        );
    }
}
>>>>>>> origin/main
