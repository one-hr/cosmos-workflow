package jp.co.onehr.workflow.base;


import jp.co.onehr.workflow.dto.base.BaseData;
import jp.co.onehr.workflow.service.base.BaseCRUDService;

public abstract class BaseCRUDServiceTest<T extends BaseData, S extends BaseCRUDService<T>> extends BaseTest {

    abstract protected Class<T> getDataClass();

    abstract protected S getService();

}
