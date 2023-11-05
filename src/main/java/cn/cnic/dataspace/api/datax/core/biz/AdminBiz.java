package cn.cnic.dataspace.api.datax.core.biz;

import cn.cnic.dataspace.api.datax.core.biz.model.HandleCallbackParam;
import cn.cnic.dataspace.api.datax.core.biz.model.HandleProcessCallbackParam;
import cn.cnic.dataspace.api.datax.core.biz.model.RegistryParam;
import cn.cnic.dataspace.api.datax.core.biz.model.ReturnT;
import java.util.List;

/**
 * @author xuxueli 2017-07-27 21:52:49
 */
public interface AdminBiz {

    // ---------------------- callback ----------------------
    /**
     * callback
     *
     * @param callbackParamList
     * @return
     */
    ReturnT<String> callback(List<HandleCallbackParam> callbackParamList);

    /**
     * processCallback
     *
     * @param processCallbackParamList
     * @return
     */
    ReturnT<String> processCallback(List<HandleProcessCallbackParam> processCallbackParamList);

    // ---------------------- registry ----------------------
    /**
     * registry
     *
     * @param registryParam
     * @return
     */
    ReturnT<String> registry(RegistryParam registryParam);

    /**
     * registry remove
     *
     * @param registryParam
     * @return
     */
    ReturnT<String> registryRemove(RegistryParam registryParam);
}
