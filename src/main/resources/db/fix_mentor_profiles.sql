DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN 
        SELECT u.id 
        FROM users u 
        JOIN user_roles ur ON u.id = ur.user_id 
        JOIN roles role ON role.id = ur.role_id 
        WHERE role.name = 'MENTOR'
    LOOP
        INSERT INTO mentor_profiles (user_id, headline, expertise, base_price, rating_avg, sessions_completed, verification_status, created_at, updated_at)
        VALUES (r.id, 'Chuyên gia MENTOR', 'Tư vấn 1-1, Review CV, Phỏng vấn thử', 300000, 5.0, 10, 'VERIFIED', NOW(), NOW())
        ON CONFLICT (user_id) DO UPDATE SET verification_status = 'verified';
    END LOOP;
    
    RAISE NOTICE 'Đã tự động tạo/cập nhật mentor_profiles (VERIFIED) cho toàn bộ tài khoản MENTOR!';
END $$;