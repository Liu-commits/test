--��ѯĳ��Ա�������� нˮ��ְλ

/*
1����ѯĳ��Ա����������Ϣ ---> out����̫��
2����ѯĳ�������е�����Ա����Ϣ ----> ���ص��Ǽ���
*/

create or replace procedure queryEmpInformation(eno in number,
                                                pename out varchar2,
                                                psal   out number,
                                                pjob   out varchar2)
is
begin
  
   select ename,sal,job into pename,psal,pjob from emp where empno=eno;                                             

end queryEmpInformation;
/
