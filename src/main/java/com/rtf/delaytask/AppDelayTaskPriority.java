package com.rtf.delaytask;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public enum AppDelayTaskPriority {

	HIGH("1"), MIDDLE("2"), LOW("3");
	private String remark;

}
