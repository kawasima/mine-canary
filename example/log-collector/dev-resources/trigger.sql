CREATE OR REPLACE TRIGGER login_log_after_insert
AFTER INSERT
   ON login_log
   FOR EACH ROW
BEGIN
   DBMS_ALERT.SIGNAL('NEW_LOGIN',
                     (cast(SYS_EXTRACT_UTC(:new.logined_at) as date)
                      - cast(from_tz(timestamp '1970-01-01 00:00:00', '00:00') as date)) * 24 * 60 * 60 || ' '
                     || :new.account || ' '
                     || :new.ip_address || ' '
                     || :new.success);
END;
/
