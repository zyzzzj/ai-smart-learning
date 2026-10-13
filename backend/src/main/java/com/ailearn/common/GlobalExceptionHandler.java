package com.ailearn.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.stream.Collectors;

/**
 * 全局异常处理:把各类异常统一转换为 {@link Result}。
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常。
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException ex, HttpServletRequest request) {
        log.warn("业务异常 [{}]: {}", request.getRequestURI(), ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }

    /**
     * @Valid 校验失败(请求体)。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(";"));
        return Result.error(400, msg.isEmpty() ? "参数校验失败" : msg);
    }

    /**
     * 表单绑定校验失败。
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException ex) {
        String msg = ex.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(";"));
        return Result.error(400, msg.isEmpty() ? "参数校验失败" : msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException ex) {
        return Result.error(400, "缺少必要参数:" + ex.getParameterName());
    }

    /**
     * 上传接口未携带文件(multipart 表单缺少 file 部分)。
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public Result<Void> handleMissingPart(MissingServletRequestPartException ex) {
        return Result.error(400, "缺少上传文件:" + ex.getRequestPartName());
    }

    /**
     * 其它 multipart 解析失败。
     */
    @ExceptionHandler(MultipartException.class)
    public Result<Void> handleMultipart(MultipartException ex) {
        return Result.error(400, "上传请求格式不正确:" + ex.getMessage());
    }

    /**
     * 唯一键冲突等数据完整性问题:返回 400 而不是 500。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("数据完整性异常:{}", ex.getMostSpecificCause().getMessage());
        return Result.error(400, "数据已存在或不满足约束:" + ex.getMostSpecificCause().getMessage());
    }

    /**
     * 未登录:由鉴权拦截器抛出。
     */
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleUnauthorized(UnauthorizedException ex) {
        return Result.error(401, ex.getMessage());
    }

    /**
     * 无权限 / 签名无效 / 链接过期:HTTP 403。
     */
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleForbidden(ForbiddenException ex) {
        return Result.error(403, ex.getMessage());
    }

    /**
     * 资源不存在:HTTP 404。
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFound(NotFoundException ex) {
        return Result.error(404, ex.getMessage());
    }

    /**
     * 兜底。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception ex, HttpServletRequest request) {
        log.error("系统异常 [{}]", request.getRequestURI(), ex);
        return Result.error(500, "系统繁忙,请稍后再试:" + ex.getClass().getSimpleName());
    }
}
