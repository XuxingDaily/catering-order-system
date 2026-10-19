package com.catering.identity.employee.mapper;

import com.github.pagehelper.Page;
import com.catering.shared.annotation.AutoFill;
import com.catering.dto.EmployeePageQueryDTO;
import com.catering.entity.Employee;
import com.catering.shared.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmployeeMapper {

    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);

    /**
     * 插入新增员工
     * @param employee
     */
    @AutoFill(value = OperationType.INSERT)
    @Insert("INSERT INTO employee(username, name, password, phone, sex, id_number, status, create_time, update_time, create_user, update_user)" +
            "VALUES " +
            "(#{username}, #{name}, #{password}, #{phone}, #{sex}, #{idNumber}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    void insert(Employee employee);

    /**
     * 员工分页查询
     * @return
     */
    Page<Employee> selectList(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 更新员工信息
     * @param employee
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Employee employee);

    /**
     * 根据id查询员工
     * @param id
     * @return
     */
    @Select("SELECT * FROM employee WHERE id = #{id}")
    Employee selectById(Integer id);

}
