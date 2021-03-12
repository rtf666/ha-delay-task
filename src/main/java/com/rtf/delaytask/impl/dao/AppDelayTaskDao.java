package com.rtf.delaytask.impl.dao;

import com.rtf.delaytask.AppDelayTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppDelayTaskDao extends JpaRepository<AppDelayTask, Long> {

    @Query(value = "UPDATE AppDelayTask SET circle = ?2 WHERE id = ?1")
    boolean updateCircle(Long id , boolean circle) ;

    @Query(value = "UPDATE AppDelayTask SET complete = ?2 WHERE id = ?1")
    boolean updateComplete(Long id , int complete) ;

}