package com.lyq.impl;

import java.util.List;

import com.lyq.entity.Students;
import com.lyq.service.StudentService;
import com.lyq.utils.StudentBusiness;

public class StudentServiceImpl implements StudentService {

	@Override
	public List<Students> getAllStudents() {
		// TODO Auto-generated method stub
		return StudentBusiness.getAllStudents();
	}

}
