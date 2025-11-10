package com.datacopilotx.ai.domian.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageVO<T> {
	private Integer pageNo;
	private Integer pageSize;
	private Long total;
	private Long totalPage;
	private T data;
}

