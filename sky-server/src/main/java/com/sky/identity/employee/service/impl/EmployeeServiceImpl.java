package com.sky.identity.employee.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.shared.constant.MessageConstant;
import com.sky.shared.constant.PasswordConstant;
import com.sky.shared.constant.StatusConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeEditPasswordDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.shared.exception.AccountLockedException;
import com.sky.shared.exception.AccountNotFoundException;
import com.sky.shared.exception.PasswordErrorException;
import com.sky.identity.employee.mapper.EmployeeMapper;
import com.sky.shared.result.PageResult;
import com.sky.identity.employee.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @Override
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    /**
     * 新增员工
     * @param employeeDTO
     */
    @Override
    public void save(EmployeeDTO employeeDTO) {
        // DTO 转换为 PO
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);
        // 默认状态开启
        employee.setStatus(StatusConstant.ENABLE);
        // 设置默认密码
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        // 传给 mapper
        employeeMapper.insert(employee);
    }

    /**
     * 员工分页查询
     * @param employeePageQueryDTO
     * @return
     */
    @Override
    public PageResult page(EmployeePageQueryDTO employeePageQueryDTO) {
        // 设置分页参数
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());

        // 执行查询
        Page<Employee> page = employeeMapper.selectList(employeePageQueryDTO);

        // 获取结果
        long total = page.getTotal();
        List<Employee> result = page.getResult().stream()
                .peek((emp) -> emp.setPassword("********"))
                .collect(Collectors.toList());

        // 封装结果
        return new PageResult(total, result);
    }

    /**
     * 启用、禁用员工账号
     * @param status
     * @param id
     */
    @Override
    public void updateStatus(Integer status, Integer id) {
        Employee employee = Employee.builder().id(Long.valueOf(id)).status(status).build();

        employeeMapper.update(employee);
    }

    /**
     * 根据id查询员工
     * @param id
     * @return
     */
    @Override
    public Employee getById(Integer id) {
        Employee employee = employeeMapper.selectById(id);
        employee.setPassword("********");
        return employee;
    }

    /**
     * 编辑员工信息
     * @param employeeDTO
     */
    @Override
    public void update(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);

        employeeMapper.update(employee);
    }

    /**
     * 修改密码
     * @param employeeEditPasswordDTO
     */
    @Override
    public void updatePassword(EmployeeEditPasswordDTO employeeEditPasswordDTO) {
        Integer id = employeeEditPasswordDTO.getEmpId();
        String oldPassword = DigestUtils.md5DigestAsHex(employeeEditPasswordDTO.getOldPassword().getBytes());

        // 核验旧密码是否正确
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (!oldPassword.equals(employee.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        // 修改用户密码
        String newPassword = DigestUtils.md5DigestAsHex(employeeEditPasswordDTO.getNewPassword().getBytes());
        employee.setPassword(newPassword);

        employeeMapper.update(employee);
    }

}
