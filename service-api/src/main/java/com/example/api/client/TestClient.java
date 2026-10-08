//package com.example.api.client;
//
//import com.example.common.domain.PageBean;
//import com.example.common.domain.Result;
//import com.example.common.domain.User;
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//
//@FeignClient(name = "test-service", path = "/api/testservice")
//public interface TestClient {
//
//    @GetMapping("/users")
//    Result<PageBean<User>> getUsers(
//            @RequestParam("page") Integer page,
//            @RequestParam("size") Integer size,
//            @RequestParam(value = "email", required = false) String email,
//            @RequestParam(value = "remark", required = false) String remark);
//
//}
