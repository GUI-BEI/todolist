package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// 这个接口继承了 JpaRepository，提供了基本的 CRUD 操作,针对的是EventItem实体，主键类型是Long
//本质是包装了对数据库表的操作，这个表由EventItem这个实体类映射而来
// 会扫描这个类来找到表名和字段来生成相关的SQL语句并包装成方法供我们调用，仅对应于EventItemDto这个实体类对应的表

@Repository
interface  UserRepository extends JpaRepository<User,Long> {
    // 根据用户名查询用户，用于登录
    User findByUsername(String username);
}