package edu.sjsu;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;

@WebServlet(name = "RestHandler", urlPatterns = { "/cs218sp19/rest/*" })
public class RestHandler extends HttpServlet {
	private static final long serialVersionUID = 1L;
	MongoClient mongo;
	MongoDatabase mongoDB;

	@Override
	public void init() throws ServletException {
		mongo = new MongoClient( "localhost" , 27017 );
		mongoDB = mongo.getDatabase("restdb");
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		Utils.setRespInitProps(resp);
		String path = req.getPathInfo();
		try {
			String[] pathElems = path.split("/");
			FindIterable<org.bson.Document> iter;
			String xmlResp = "";
			if (pathElems.length == 2) {
				iter = Utils.getEntities(pathElems[1], null, mongoDB);
				xmlResp = Utils.getXMLResponse(pathElems[1], null, iter);
			} else if (pathElems.length == 3) {
				iter = Utils.getEntities(pathElems[1], pathElems[2], mongoDB);
				xmlResp = Utils.getXMLResponse(pathElems[1], pathElems[2], iter);
			}
			if (!xmlResp.isEmpty()) {
				resp.setStatus(HttpServletResponse.SC_OK);
				resp.getWriter().write(xmlResp);
			} else {
				resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}

		} catch (Exception e) {
			resp.setStatus(Utils.exceptionHandler(e));
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Utils.setRespInitProps(resp);
		String path = req.getPathInfo();
		try {
			String body = Utils.getBody(req);
			Map<String, String> map = Utils.getInnerMap(body);
			String kind;
			if (path.equals("/employee")) {
				kind = "employee";
			} else if (path.equals("/project")) {
				kind = "project";
			} else {
				kind = "";
				resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
			if (!kind.isEmpty()) {
				org.bson.Document newEntity = Utils.createEntity(kind, "", map);
				org.bson.Document existEntity = Utils.entityExists(kind, newEntity, mongoDB);
				if (existEntity != null) {
					throw new IllegalArgumentException("409");
				}
				Utils.persistEntity(kind, newEntity, "INSERT", mongoDB);
				resp.setStatus(HttpServletResponse.SC_CREATED);
				String loc = req.getRequestURL().toString() + "/" + map.get("id");
				resp.setHeader("Location", loc);
			}
		} catch (Exception e) {
			resp.setStatus(Utils.exceptionHandler(e));
		}
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Utils.setRespInitProps(resp);
		String path = req.getPathInfo();
		try {
			String[] pathElems = path.split("/");
			if (pathElems.length == 3) {
				String body = Utils.getBody(req);
				Map<String, String> map = Utils.getInnerMap(body);
				map.put("id", pathElems[2]);
				String kind;
				if (pathElems[1].equals("employee")) {
					kind = "employee";
				} else if (pathElems[1].equals("project")) {
					kind = "project";
				} else {
					kind = "";
					resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
				}
				if (!kind.isEmpty()) {
					org.bson.Document newEntity = Utils.createEntity(kind, "", map);
					org.bson.Document existEntity = Utils.entityExists(kind, newEntity, mongoDB);
					if (existEntity != null) {
						Map<String, Object> existMap = existEntity;
						for (Map.Entry<String, Object> entry : existMap.entrySet()) {
							if (!map.containsKey(entry.getKey())) {
								map.put(entry.getKey(), entry.getValue().toString());
							}
						}
						org.bson.Document updateEntity = Utils.createEntity(kind, "", map);
						Utils.persistEntity(kind, updateEntity, "UPDATE", mongoDB);
						resp.setStatus(HttpServletResponse.SC_OK);
					} else {
						resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
					}
				}
			} else {
				throw new IllegalArgumentException("400");
			}
		} catch (Exception e) {
			resp.setStatus(Utils.exceptionHandler(e));
		}
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Utils.setRespInitProps(resp);
		String path = req.getPathInfo();
		try {
			String[] pathElems = path.split("/");
			if (pathElems.length == 3) {
				Map<String, String> map = new HashMap<>();
				map.put("id", pathElems[2]);
				String kind;
				if (pathElems[1].equals("employee")) {
					kind = "employee";
				} else if (pathElems[1].equals("project")) {
					kind = "project";
				} else {
					kind = "";
					resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
				}
				if (!kind.isEmpty()) {
					org.bson.Document newEntity = Utils.createEntity(kind, "", map);
					org.bson.Document existEntity = Utils.entityExists(kind, newEntity, mongoDB);
					if (existEntity != null) {
						Utils.deleteEntity(kind,existEntity, mongoDB);
						resp.setStatus(HttpServletResponse.SC_OK);
					} else {
						resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
					}
				}
			} else {
				throw new IllegalArgumentException("400");
			}
		} catch (Exception e) {
			resp.setStatus(Utils.exceptionHandler(e));
		}
	}

}