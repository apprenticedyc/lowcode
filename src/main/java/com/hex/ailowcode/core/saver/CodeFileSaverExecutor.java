package com.hex.ailowcode.core.saver;

import com.hex.ailowcode.ai.model.HtmlCodeResult;
import com.hex.ailowcode.ai.model.MultiFileCodeResult;
import com.hex.ailowcode.exception.BusinessException;
import com.hex.ailowcode.exception.ErrorCode;
import com.hex.ailowcode.model.enums.CodeGenTypeEnum;

import java.io.File;

/**
 * 保存解析后的代码到文件中
 * 执行器设计模式 -- 根据枚举值不同选择不同的保存路径
 */
public class CodeFileSaverExecutor {

    private static final HtmlCodeFileSaver htmlCodeFileSaver = new HtmlCodeFileSaver();

    private static final MultiFileCodeFileSaver multiFileCodeFileSaver = new MultiFileCodeFileSaver();

    /**
     * 执行代码保存（使用 appId）
     *
     * @param codeResult  代码结果对象
     * @param codeGenType 代码生成类型
     * @param appId       应用 ID
     * @return 保存的目录
     */
    public static File executeSaver(Object codeResult, CodeGenTypeEnum codeGenType, Long appId) {
        return switch (codeGenType) {
            case HTML -> htmlCodeFileSaver.saveCode((HtmlCodeResult) codeResult, appId);
            case MULTI_FILE -> multiFileCodeFileSaver.saveCode((MultiFileCodeResult) codeResult, appId);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType);
        };
    }
}