rem PL/SQL Developer Test Script

set feedback off
set autoprint off

rem Execute PL/SQL Block
-- ��ӡ1~10
declare 
  -- �������
  pnum number := 1;
begin
  loop
    --��������
    exit when pnum > 10;
    
    --��ӡ
    dbms_output.put_line(pnum);
    --��һ
    pnum := pnum + 1;
  end loop;
end;
/
