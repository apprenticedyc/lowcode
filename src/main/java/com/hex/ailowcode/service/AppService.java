package com.hex.ailowcode.service;

import com.hex.ailowcode.model.dto.ai.AppAddRequest;
import com.hex.ailowcode.model.dto.ai.AppQueryRequest;
import com.hex.ailowcode.model.dto.ai.AppUpdateRequest;
import com.hex.ailowcode.model.entity.App;
import com.hex.ailowcode.model.entity.User;
import com.hex.ailowcode.model.vo.AppVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 */
public interface AppService extends IService<App> {

    // ==================== VO 转换相关 ====================

    /**
     * 获取应用封装类
     *
     * @param app 应用实体
     * @return 应用 VO
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用封装类列表
     *
     * @param appList 应用实体列表
     * @return 应用 VO 列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 根据 id 获取应用 VO
     *
     * @param id 应用 id
     * @return 应用 VO
     */
    AppVO getAppVOById(Long id);

    // ==================== 查询相关 ====================

    /**
     * 构造应用查询条件
     *
     * @param appQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 分页获取当前用户的应用 VO 列表
     *
     * @param appQueryRequest 查询请求
     * @param loginUser       登录用户
     * @return 应用 VO 分页结果
     */
    Page<AppVO> listMyAppVOByPage(AppQueryRequest appQueryRequest, User loginUser);

    /**
     * 分页获取精选应用 VO 列表
     *
     * @param appQueryRequest 查询请求
     * @return 应用 VO 分页结果
     */
    Page<AppVO> listGoodAppVOByPage(AppQueryRequest appQueryRequest);

    /**
     * 管理员分页获取应用 VO 列表
     *
     * @param appQueryRequest 查询请求
     * @return 应用 VO 分页结果
     */
    Page<AppVO> listAppVOByPageByAdmin(AppQueryRequest appQueryRequest);

    // ==================== 增删改相关 ====================

    /**
     * 创建应用
     *
     * @param appAddRequest 创建请求
     * @param loginUser     登录用户
     * @return 应用 id
     */
    Long addApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 更新应用（用户只能更新自己的应用名称）
     *
     * @param appUpdateRequest 更新请求
     * @param loginUser       登录用户
     * @return 更新结果
     */
    Boolean updateApp(AppUpdateRequest appUpdateRequest, User loginUser);

    /**
     * 删除应用
     *
     * @param id        应用 id
     * @param loginUser 登录用户
     * @return 删除结果
     */
    Boolean deleteApp(Long id, User loginUser);

    /**
     * 管理员删除应用
     *
     * @param id 应用 id
     * @return 删除结果
     */
    Boolean deleteAppByAdmin(Long id);

    // ==================== AI 生成相关 ====================

    /**
     * 通过对话生成应用代码（流式）
     *
     * @param appId     应用 ID
     * @param message   提示词
     * @param loginUser 登录用户
     * @return 代码内容流
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);


    /**
     * 应用部署
     *
     * @param appId 应用 ID
     * @param loginUser 登录用户
     * @return 可访问的部署地址
     */
    String deployApp(Long appId, User loginUser);
}