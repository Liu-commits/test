rem PL/SQL Developer Test Script

set feedback off
set autoprint off

rem Execute PL/SQL Block
-- �ж��û��Ӽ������������

--���ܼ�������
--����num����һ����ֵַ���ڸõ�ַ�ϱ����������ֵ
accept num prompt '������һ������';

declare 
  --��������������� ������
  pnum number := &num;
begin
  if pnum = 0 then dbms_output.put_line('���������0');
     elsif pnum = 1 then dbms_output.put_line('���������1');
     elsif pnum = 2 then dbms_output.put_line('���������2');
     else dbms_output.put_line('��������');
  end if;
end;
/
