rem PL/SQL Developer Test Script

set feedback off
set autoprint off

rem Execute PL/SQL Block
--��¼�ͱ���: ��ѯ����ӡ7839��������нˮ

declare
  --�����¼�ͱ���������һ��
  emp_rec emp%rowtype;
begin
  select * into emp_rec from emp where empno=7839;
  
  dbms_output.put_line(emp_rec.ename||'��нˮ��'||emp_rec.sal);
end;
/
