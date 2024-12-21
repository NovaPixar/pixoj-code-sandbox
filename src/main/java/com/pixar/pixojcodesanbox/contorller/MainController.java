package com.pixar.pixojcodesanbox.contorller;

import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import com.pixar.pixojcodesanbox.JavaNativeCodeSandbox;
import com.pixar.pixojcodesanbox.model.ExecuteCodeRequest;
import com.pixar.pixojcodesanbox.model.ExecuteCodeResponse;
import com.pixar.pixojcodesanbox.utils.CryptoUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Base64;

@RestController("/")
public class MainController {
    //定义鉴权请求头和密钥
    private static final String AUTH_REQUEST_HEADER = "auth";

    private static final String AUTH_REQUEST_SECRET = "secretKey";

    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;

    private static SecretKey secretKey;

    @GetMapping("/health")
    public String healthCheck() {
        return "OK";
    }

    /**
     * 执行代码
     *
     * @param
     * @return
     */
    @PostMapping("/executeCode")
    ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request, HttpServletResponse response){
        // 进行一个基本的验证
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);

        if(!AUTH_REQUEST_SECRET.equals(authHeader)){
            response.setStatus(403);
            return null;
        }
        if(executeCodeRequest == null){
            throw new RuntimeException("参数为空");
        }
        return javaNativeCodeSandbox.executeCode(executeCodeRequest);

    }



}
