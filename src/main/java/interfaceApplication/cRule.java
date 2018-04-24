package interfaceApplication;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import common.java.JGrapeSystem.rMsg;
import common.java.apps.appsProxy;
import common.java.interfaceModel.GrapeDBDescriptionModel;
import common.java.interfaceModel.GrapePermissionsModel;
import common.java.interfaceModel.GrapeTreeDBModel;
import common.java.security.codec;
import common.java.session.session;
import common.java.string.StringHelper;
import common.java.time.timeHelper;

public class cRule {
	private JSONObject userinfo;
	private String currentUserID ;
	private GrapeTreeDBModel gDbModel;
	private String pkString;

	public cRule() {
		
		gDbModel = new GrapeTreeDBModel();
		//数据模型
		GrapeDBDescriptionModel  gdbField = new GrapeDBDescriptionModel ();
        gdbField.importDescription(appsProxy.tableConfig("cRule"));
        gDbModel.descriptionModel(gdbField);
        
        //权限模型
        GrapePermissionsModel gperm = new GrapePermissionsModel();
		gperm.importDescription(appsProxy.tableConfig("cRule"));
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

	/**
	 * 添加规则
	 * 
	 * @project GrapeInfoCollection
	 * @package interfaceApplication
	 * @file CollectionService.java
	 * 
	 * @param ruleInfo
	 * @param Result
	 * @return 返回新增的规则id
	 *
	 */
	@SuppressWarnings("unchecked")
	protected String AddRule(String ruleInfo, String Result) {
		Object info = null;
		JSONObject object = JSONObject.toJSON(Result);
		JSONObject rules = JSONObject.toJSON(ruleInfo);
		JSONObject rule = new JSONObject();
		if (object != null && object.size() > 0) {
			if (Integer.parseInt(object.getString("errorcode")) == 0) {
				rule.put("userid", currentUserID);
				rule.put("time", timeHelper.nowMillis());
//				rules = encodes(rules);
				rule.put("rule", (rules != null && rules.size() > 0) ? rules.toJSONString() : "");
				info = gDbModel.data(rule).insertOnce();
			}
		}
		return info == null ? null : info.toString();
	}

	/**
	 * 解码
	 * 
	 * @project GrapeInfoCollection
	 * @package interfaceApplication
	 * @file cRule.java
	 * 
	 * @param rules
	 * @return
	 *
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	private JSONObject encodes(JSONObject rules) {
		if (rules != null && rules.size() > 0) {
			JSONObject startURL = JSONObject.toJSON(rules.getString("startURL"));
			if (startURL != null && startURL.size() > 0) {
				String baseURL = startURL.getString("baseURL");
				startURL.put("baseURL", codec.DecodeHtmlTag(baseURL));
				rules.put("startURL", startURL);
			}
		}
		return rules;
	}

	protected JSONObject find(String ruleInfo) {
		JSONObject temp, object = null;
		temp = JSONObject.toJSON(ruleInfo);
//		temp = encodes(temp);
		if (temp != null && temp.size() > 0) {
			object = gDbModel.eq("rule", temp.toJSONString()).eq("userid", currentUserID).find();
		}
		return object != null && object.size() > 0 ? object : null;
	}

	/**
	 * 删除规则
	 * 
	 * @project GrapeInfoCollection
	 * @package interfaceApplication
	 * @file cRule.java
	 * 
	 * @param rId
	 * @return
	 *
	 */
	public String RemoveRule(String ruleId) {
		// 删除规则相关的所有内容数据
		String result = new cInfo().delete(ruleId);
		// 删除规则
		if (JSONObject.toJSON(result).getString("errorcode").equals("0")) {
			String[] value = null;
			if (ruleId != null && !ruleId.equals("")) {
				value = ruleId.split(",");
				if (value != null) {
					gDbModel.or();
					for (String rId : value) {
						gDbModel.eq(pkString, rId);
					}
				}
				@SuppressWarnings("unused")
				long code = gDbModel.deleteAll();
				result = rMsg.netMSG(0, "删除成功");
			}
		}
		return result;
	}

	/**
	 * 查询所有规则信息
	 * 
	 * @project GrapeInfoCollection
	 * @package interfaceApplication
	 * @file CollectionService.java
	 * 
	 * @param idx
	 * @param pageSize
	 * @return
	 *
	 */
	public String ShowRule(int idx, int pageSize) {
		long total = 0, totalSize = 0;
		gDbModel.eq("userid", currentUserID);
		JSONArray array = gDbModel.dirty().desc("time").desc(pkString).page(idx, pageSize);
		total = gDbModel.dirty().count();
		totalSize = gDbModel.pageMax(pageSize);
		return pageShow(array, total, totalSize, idx, pageSize);
	}

	@SuppressWarnings("unchecked")
	protected JSONObject findRule(String ruleId) {
		String[] value = null;
		JSONArray array = null;
		JSONObject object = new JSONObject();
		JSONObject temp;
		String id, rule;
//		if (ruleId != null && !ruleId.equals("")) {//TODO 1
		if (!StringHelper.InvaildString(ruleId)) {
			value = ruleId.split(",");
			if (value != null) {
				gDbModel.or();
				for (String rId : value) {
					gDbModel.eq(pkString, rId);
				}
				array = gDbModel.select();
			}
		}
		if (array != null && array.size() > 0) {
			for (Object obj : array) {
				temp = (JSONObject) obj;
				id = JSONObject.toJSON(temp.getString(pkString)).getString("$oid");
				rule = temp.getString("rule");
				object.put(id, rule);
			}
		}
		return object;
	}
	
	@SuppressWarnings("unchecked")
	public String pageShow(JSONArray array, long total, long totalSize, int idx, int pageSize) {
		JSONObject objects = new JSONObject();
		JSONObject object = new JSONObject();
		object.put("data", array);
		object.put("total", total);
		object.put("totalSize", totalSize);
		object.put("currentPage", idx);
		object.put("pageSize", pageSize);
		objects.put("records", object);
		return resultMessage(object);
	}
	
	public String resultMessage(JSONObject object) {
		if (object == null || object.size() <= 0) {
			object = new JSONObject();
		}
		return rMsg.netMSG(0, object);
	}
}
