/*
���ݵ�ȷ��
�Ǻ��нˮ����������ǰ��нˮ
*/
create or replace trigger checksalary
before update
on emp
for each row
begin
    --if �Ǻ��нˮ < ��ǰ��нˮ then
    if :new.sal < :old.sal then
       raise_application_error(-20002,'�Ǻ��нˮ����������ǰ��нˮ����ǰ:'||:old.sal||'   �Ǻ�:'||:new.sal);
    end if;
end checksalary;
/
