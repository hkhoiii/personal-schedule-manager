package com.dacn.schedule.common.exception;

/**
 * Không tìm thấy đối tượng theo id (hoặc đối tượng không thuộc người dùng hiện tại).
 *
 * <pre>{@code
 * return taskRepository.findById(id)
 *         .orElseThrow(() -> new NotFoundException("công việc", id));
 * }</pre>
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String resource, Object id) {
        super(ErrorCode.NOT_FOUND, "Không tìm thấy " + resource + " với id " + id + ".");
    }

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
