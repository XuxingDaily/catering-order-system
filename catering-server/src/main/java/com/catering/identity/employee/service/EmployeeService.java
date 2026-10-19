package com.catering.identity.employee.service;

import com.catering.dto.EmployeeDTO;
import com.catering.dto.EmployeeEditPasswordDTO;
import com.catering.dto.EmployeeLoginDTO;
import com.catering.dto.EmployeePageQueryDTO;
import com.catering.entity.Employee;
import com.catering.shared.result.PageResult;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 新增员工
     * @param employeeDTO
     */
    void save(EmployeeDTO employeeDTO);

    /**
     * 员工分页查询
     * @param employeePageQueryDTO
     */
    PageResult page(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 启用、禁用员工账号
     * @param status
     * @param id
     */
    void updateStatus(Integer status, Integer id);

    /**
     * 根据id查询员工
     * @param id
     * @return
     */
    Employee getById(Integer id);

    /**
     * 编辑员工信息
     * @param employeeDTO
     */
    void update(EmployeeDTO employeeDTO);

    /**
     * 修改密码
     * @param employeeEditPasswordDTO
     */
    void updatePassword(EmployeeEditPasswordDTO employeeEditPasswordDTO);

}
