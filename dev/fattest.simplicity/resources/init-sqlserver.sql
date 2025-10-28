-- Create database
CREATE DATABASE TEST;

-- Enable XA connections
EXEC sp_sqljdbc_xa_install;

-- Enable all users to use distributed transactions
GRANT EXECUTE ON xp_sqljdbc_xa_init to public
GRANT EXECUTE ON xp_sqljdbc_xa_start to public
GRANT EXECUTE ON xp_sqljdbc_xa_end to public
GRANT EXECUTE ON xp_sqljdbc_xa_prepare to public
GRANT EXECUTE ON xp_sqljdbc_xa_commit to public
GRANT EXECUTE ON xp_sqljdbc_xa_rollback to public
GRANT EXECUTE ON xp_sqljdbc_xa_recover to public
GRANT EXECUTE ON xp_sqljdbc_xa_forget to public
GRANT EXECUTE ON xp_sqljdbc_xa_rollback_ex to public
GRANT EXECUTE ON xp_sqljdbc_xa_forget_ex to public
GRANT EXECUTE ON xp_sqljdbc_xa_prepare_ex to public
GRANT EXECUTE ON xp_sqljdbc_xa_init_ex to public

-- Allow snapshot isolation
ALTER DATABASE TEST SET ALLOW_SNAPSHOT_ISOLATION ON

-- Enable case sensitive collation based on binary representation of
-- data to replicate default behavior of other databases
ALTER DATABASE TEST COLLATE Latin1_General_bin;