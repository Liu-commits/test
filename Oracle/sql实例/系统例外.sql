rem PL/SQL Developer Test Script

set feedback off
set autoprint off

rem Execute PL/SQL Block
-- ��0��
declare
   pnum number;
begin
  pnum := 1/0;
  
exception
  when zero_divide then dbms_output.put_line('1:0��������ĸ');
                        dbms_output.put_line('2:0��������ĸ');
  when value_error then dbms_output.put_line('��������ת������');                      
  when others then dbms_output.put_line('��������');
end;
/
