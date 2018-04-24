package interfaceApplication;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import common.java.JGrapeSystem.rMsg;
import common.java.check.checkHelper;
import common.java.httpClient.request;
import common.java.nlogger.nlogger;
import common.java.security.codec;
import common.java.string.StringHelper;

public class CollectInfo {
	private cInfo cInfo = new cInfo();

	/**
	 * 获取采集信息
	 * 
	 * @project GrapeInfoCollection
	 * @package interfaceApplication
	 * @file CollectInfo.java
	 * 
	 * @param baseurl
	 *            格式{"startURL":{"baseURL":"URL","select":{"URL1":"select1",
	 *            "URL2":"select2","URLN":"selectN"}},"isRemoveHtml":0,
	 *            "isrunning":0}
	 * @throws Exception
	 *
	 */
	public String getInfo(String baseurl) throws Exception {
		String url = "", rId;
		int isRemoveHtml = 0;
		String nextURL = "";
		String contentConfig = "", data = "";
		JSONArray array = null;
		String result = rMsg.netMSG(99, "获取信息失败");
		// 判断规则是否存在
		baseurl = codec.DecodeHtmlTag(baseurl);
		baseurl = codec.decodebase64(baseurl);
		JSONObject temp = new cRule().find(baseurl);
		if (temp != null && temp.size() > 0) {
			rId = JSONObject.toJSON(temp.getString("_id")).getString("$oid");
			return new cInfo().searchByRID(rId);
		}
		JSONObject object = JSONObject.toJSON(baseurl);
		String invoke = object.getString("invoke");
		if (object != null && object.size() > 0) {
			if (object.containsKey("startURL")) {
				url = object.getString("startURL");
				if (url != null && !url.equals("") && !url.equals("null")) {
					url = getStartUrl(url); // 循环开始url
					if (url == null) {
						return rMsg.netMSG(90, "输入url地址有误");
					}
				}
				if (object.containsKey("isRemoveHtml")) {
					isRemoveHtml = Integer.parseInt(object.getString("isRemoveHtml")); // 是否去除html标签，0：表示不去除；1：表示去除
				}
				if (object.containsKey("nextURL")) {
					nextURL = object.getString("nextURL"); // 下一页url
				}
				if (object.containsKey("contentConfig")) {
					contentConfig = object.getString("contentConfig");
				}
			}
			// 获取下一页url
			url = getNextUrl(url, nextURL);
			if (url != null && !url.equals("")) {
				array = getContent(url, contentConfig, isRemoveHtml);
				result = resultMessage(array);
				object = JSONObject.toJSON(result);
				if (object != null && object.size() > 0) {
					data = JSONObject.toJSON(object.getString("message")).getString("records");
				}
				invokes(invoke, data);
				// 添加内容到表中
				result = cInfo.AddContent(array, baseurl, result);
			}
		}
		return result;
	}

	/**
	 * 远程调用
	 * @project	GrapeInfoCollection
	 * @package interfaceApplication
	 * @file CollectInfo.java
	 * 
	 * @param invoke
	 * @param info
	 * @return
	 *
	 */
	private String invokes(String invoke, String info) {
		String result = rMsg.netMSG(90, "回调地址异常");
		String temp;
		String infos = info;
		if (info != null && !info.equals("")) {
			if (!invoke.contains("http")) {
				if (!invoke.endsWith("/")) {
					infos = "/" + info;
				}
				invoke = invoke + infos;
				temp = request.Get(invoke);
				if (temp !=null && !temp.equals("")) {
					result = rMsg.netMSG(0, "操作成功");
				}
			}
		}else{
			result = rMsg.netMSG(90, "回调地址异常");
		}
		return result;
	}

	/**
	 * 获取内容信息
	 * 
	 * @project GrapeInfoCollection
	 * @package interfaceApplication
	 * @file CollectInfo.java
	 * 
	 * @param url
	 *
	 */
	@SuppressWarnings("unchecked")
	private JSONArray getContent(String url, String contentConfig, int isRemoveHtml) {
		JSONObject webPageContent = null;
		JSONObject content = JSONObject.toJSON(contentConfig);
		// JSONObject object = new JSONObject();
		int index = 0;
		JSONArray array = null;
		Document document = null;
		Elements elements = null;
		String[] urls = null, values = null;
		;
		String key, value, temp = "";
//		if (url != null && !url.equals("")) {//TODO 1
		if (!StringHelper.InvaildString(url)) {
			try {
				array = new JSONArray();
				urls = url.split(",");
				for (String netURL : urls) {
					webPageContent = new JSONObject();
					if (netURL != null && !netURL.equals("") && !netURL.equals("null")) {
						document = Jsoup.connect(netURL).get();
						if (content != null && content.size() > 0) {
							// content格式为{"key":"value:index"}
							for (Object obj : content.keySet()) {
								key = obj.toString();
								value = content.getString(key); // value:index
								if (value.contains(":")) {
									values = value.split(":"); //
									value = values[0]; // value
									temp = values[1]; // value
									if (checkHelper.isNum(temp)) {
										index = Integer.parseInt(temp);
									}
								}
								if (value != null && !value.equals("") && !value.equals("null")) {
									elements = document.select(value);
									if (!elements.isEmpty()) {
										temp = isRemoveHtml == 1 ? elements.get(index).text() : elements.get(0).toString();
									}
								}
								webPageContent.put(key, temp);
							}
						}
						array.add(webPageContent);
					}
				}
			} catch (Exception e) {
				nlogger.logout(e);
				array = null;
			}
		}
		return (array != null && array.size() > 0) ? array : null;
	}

	/**
	 * 连续获取下一页url
	 * 
	 * @project GrapeInfoCollection
	 * @package interfaceApplication
	 * @file CollectInfo.java
	 * 
	 * @param startURL
	 * @param url
	 * @return
	 *
	 */
	private String getNextUrl(String startURL, String url) {
		if (startURL == null || startURL.equals("") || startURL.equals("null")) {
			return null;
		}
		String temp = startURL;
		try {
			Document document = null;
			Elements element = null;
			while (temp != null && !temp.equals("")) {
				document = Jsoup.connect(temp).get();
				element = document.select(url);
				if (!element.isEmpty()) {
					temp = element.get(0).attr("abs:href");
				} else {
					temp = null;
				}
				startURL += "," + temp;
				startURL = StringHelper.fixString(startURL, ',');
			}
		} catch (Exception e) {
			nlogger.logout(e);
			startURL = null;
		}
		return startURL;
	}

	/**
	 * 获取循环开始URL
	 * 
	 * @project GrapeInfoCollection
	 * @package interfaceApplication
	 * @file CollectInfo.java
	 * 
	 * @param baseurl
	 * 
	 * @return {"baseURL":"URL","select":{"URL1":"select1","URL2":"select2",
	 *         "URLN":"selectN"}}
	 * @throws Exception
	 *
	 */
	private String getStartUrl(String baseurl) throws Exception {
		String url = "", temp;
		// String key, value;
		Document document = null;
		Elements element = null;
		// 获取开始url
		JSONObject object = JSONObject.toJSON(baseurl);
		try {
			if (object != null && object.size() > 0) {
				url = object.getString("baseURL");
				String selector = object.getString("select");
				if (!selector.equals("")) {
					String[] values = selector.split(",");
					for (int i = 0; i < values.length; i++) {
						temp = values[i];
						if (!temp.equals("")) {
							document = Jsoup.connect(url).get();
							element = document.select(temp);
							if (!element.isEmpty()) {
								url = element.get(0).attr("abs:href");
							}
						}
					}
				}
			}
		} catch (Exception e) {
			nlogger.logout(e);
			url = null;
		}

		// JSONObject object2 =
		// JSONObject.toJSON(object.getString("select"));
		// int count = 1;
		// key = "URL" + count;
		// value = object2.getString(key);
		// if (value != null && !value.equals("") && !value.equals("null"))
		// {
		// document = Jsoup.connect(url).get();
		// element = document.select(value);
		// url = element.get(0).attr("abs:href");
		// }
		// count++;
		return url;
	}
	
	@SuppressWarnings("unchecked")
	public String resultMessage(JSONArray array) {
		JSONObject object = new JSONObject();
		if (array == null || array.size() <= 0) {
			array = new JSONArray();
		}
		object.put("records", array);
		return resultMessage(object);
	}
	
	public String resultMessage(JSONObject object) {
		if (object == null || object.size() <= 0) {
			object = new JSONObject();
		}
		return rMsg.netMSG(0, object);
	}
}
