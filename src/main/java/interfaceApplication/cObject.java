package interfaceApplication;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import common.java.apps.appsProxy;
import common.java.interfaceModel.GrapeDBDescriptionModel;
import common.java.interfaceModel.GrapePermissionsModel;
import common.java.interfaceModel.GrapeTreeDBModel;
import common.java.session.session;
import common.java.string.StringHelper;

public class cObject {
	
	private String currentUserID;
	private GrapeTreeDBModel gDbModel;
	private String pkString;
	private JSONObject userinfo;

	public cObject() {
		
		gDbModel = new GrapeTreeDBModel();
		//数据模型
		GrapeDBDescriptionModel  gdbField = new GrapeDBDescriptionModel ();
        gdbField.importDescription(appsProxy.tableConfig("cObject"));
        gDbModel.descriptionModel(gdbField);
        
        //权限模型
        GrapePermissionsModel gperm = new GrapePermissionsModel();
		gperm.importDescription(appsProxy.tableConfig("cObject"));
		gDbModel.permissionsModel(gperm);
		
		pkString = gDbModel.getPk();
		
        //开启检查模式
        gDbModel.checkMode();
        
        //用户信息
  		userinfo = (new session()).getDatas();
  		if (userinfo != null && userinfo.size() > 0) {
  			currentUserID = (String) userinfo.getPkValue(pkString);
  		}
	}

	@SuppressWarnings("unchecked")
	protected String AddObject(JSONArray array) {
		String cid = "";
		Object temp = null;
		JSONObject objs = new JSONObject();
		JSONObject object = new JSONObject();
		if (array != null && array.size() > 0) {
			for (Object obj : array) {
				object = (JSONObject) obj;
				if (object != null && object.size() > 0) {
					objs.put("content", object.toJSONString());
					objs.put("userid", currentUserID);
					temp = gDbModel.data(objs).insertOnce();
					if (temp != null) {
						cid += temp + ",";
					}
				}
			}
		}
		return StringHelper.fixString(cid, ',');
	}

	@SuppressWarnings("unchecked")
	protected JSONObject findObject(String infoId) {
		String[] value = null;
		JSONArray array = null;
		JSONObject temp, object = null, content = null;
		String ids;
		if (infoId != null && !infoId.equals("")) {
			value = infoId.split(",");
			if (value != null) {
				gDbModel.or();
				for (String id : value) {
					gDbModel.eq(pkString, id);
				}
				array = gDbModel.field("content").select();
			}
		}
		if (array != null && array.size() > 0) {
			object = new JSONObject();
			for (Object obj : array) {
				temp = (JSONObject) obj;
				ids = JSONObject.toJSON(temp.getString("_id")).getString("$oid");
				content = JSONObject.toJSON(temp.getString("content"));
				object.put(ids, (content == null) ? new JSONObject() : content);
			}
		}
		return object;
	}
}
