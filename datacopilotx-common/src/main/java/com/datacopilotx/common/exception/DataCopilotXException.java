package com.datacopilotx.common.exception;


import com.datacopilotx.common.result.ResponseCode;
import lombok.Getter;

import java.io.Serial;

@Getter
public class DataCopilotXException extends RuntimeException {
	@Serial
	private static final long serialVersionUID = -940285811464169752L;
	private ResponseCode code;

	public DataCopilotXException(String msg) {
		super(msg);
	}

	public DataCopilotXException(ResponseCode code) {
		super(code.getMsg());
		this.code = code;
	}

	public DataCopilotXException(ResponseCode code, String msg) {
		super(msg);
		this.code = code;
	}
}
