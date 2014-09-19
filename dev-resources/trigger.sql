CREATE OR REPLACE TRIGGER login_log_after_insert
AFTER INSERT
   ON login_log
   FOR EACH ROW
BEGIN
   DBMS_ALERT.SIGNAL('NEW_LOGIN', :new.LOGIN_ID || ' ' || :new.ACCESS_DATE || ' '
   || :new.ACCESS_IP_ADDRESS || ',' || :new.SUCCESS);
END;
/
