package com.datacopilotx.common.util;


import java.util.UUID;


public final class IdUtils {
	private IdUtils() {
	}

	/**
	 * 生成业务ID：folder_xxx， tb_xxx
	 *
	 * @param prefix
	 * @return
	 */
	public static String genKey(String prefix) {
		String uuid = UUID.randomUUID().toString().replace("-", "");
		return String.format("%s_%s", prefix, uuid);
	}
}
