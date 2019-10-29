package edu.sjsu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.rmi.ServerException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

public class Utils {
	public static String getBody(HttpServletRequest req) throws IOException {
		BufferedReader br = req.getReader();
		return br.lines().collect(Collectors.joining(System.lineSeparator()));
	}

	public static FindIterable<org.bson.Document> getEntities(String kind, String id, MongoDatabase mongoDB)
			throws NumberFormatException, ServerException {
		if (kind.equalsIgnoreCase("employee"))
			kind = "employee";
		else if (kind.equalsIgnoreCase("project"))
			kind = "project";
		FindIterable<org.bson.Document> iterable;
		MongoCollection<org.bson.Document> col = mongoDB.getCollection(kind);
		BasicDBObject q;
		if (id != null) {
			int ident = Integer.parseInt(id);
			q = new BasicDBObject("id", new BasicDBObject("$eq", ident));
		} else {
			q = new BasicDBObject();
		}
		if (col != null) {
			iterable = col.find(q);
		} else {
			throw new ServerException("500");
		}
		return iterable;
	}

	public static String getXMLResponse(String kind, String id, FindIterable<org.bson.Document> iter) {
		String xml = "";
		MongoCursor<org.bson.Document> mongoCur = iter.iterator();
		if (kind.equalsIgnoreCase("employee")) {
			while (mongoCur.hasNext()) {
				org.bson.Document doc = mongoCur.next();
				Map<String, Object> map = doc;
				xml += "<employee>\n";
				xml += "<id>" + map.get("id") + "</id>\n";
				if (map.containsKey("firstName"))
					xml += "<firstName>" + map.get("firstName") + "</firstName>\n";
				if (map.containsKey("lastName"))
					xml += "<lastName>" + map.get("lastName") + "</lastName>\n";
				xml += "</employee>";
			}
			if (id == null && !xml.isEmpty()) {
				xml = "<employeeList>\n" + xml + "\n</employeeList>";
			}
		} else if (kind.equalsIgnoreCase("project")) {
			while (mongoCur.hasNext()) {
				org.bson.Document doc = mongoCur.next();
				Map<String, Object> map = doc;
				xml += "<project>\n";
				xml += "<id>" + map.get("id") + "</id>\n";
				if (map.containsKey("name"))
					xml += "<name>" + map.get("name") + "</name>\n";
				if (map.containsKey("budget"))
					xml += "<budget>" + map.get("budget") + "</budget>\n";
				xml += "</project>";
			}
			if (id == null && !xml.isEmpty()) {
				xml = "<projectList>\n" + xml + "\n</projectList>";
			}
		}
		return xml;
	}

	public static Map<String, String> getInnerMap(String body) throws DocumentException {
		Map<String, String> map = new HashMap<String, String>();
		Document doc = new SAXReader().read(new StringReader(body));
		List<Element> elements = doc.getRootElement().elements();
		if (elements != null) {
			for (int i = 0; i < elements.size(); i++) {
				map.put(elements.get(i).getName(), elements.get(i).getText());
			}
		}
		return map;
	}

	public static org.bson.Document createEntity(String kind, String key, Map<String, String> map)
			throws IllegalArgumentException, NumberFormatException {
		if (!map.containsKey("id")) {
			throw new IllegalArgumentException("400");
		}
		int id = Integer.parseInt(map.get("id"));
		org.bson.Document doc = new org.bson.Document();
		if (kind.equals("employee")) {
			doc.append("id", id);
			if (map.containsKey("firstName"))
				doc.append("firstName", map.get("firstName"));
			if (map.containsKey("lastName"))
				doc.append("lastName", map.get("lastName"));
		} else if (kind.equals("project")) {
			doc.append("id", id);
			if (map.containsKey("name"))
				doc.append("name", map.get("name"));
			if (map.containsKey("budget"))
				doc.append("budget", Float.parseFloat(map.get("budget")));
		}
		return doc;
	}

	public static org.bson.Document entityExists(String kind, org.bson.Document entity, MongoDatabase mongoDB)
			throws ServerException {
		if (mongoDB != null) {
			FindIterable<org.bson.Document> iter = Utils.getEntities(kind, entity.get("id").toString(), mongoDB);
			if (iter.iterator().hasNext()) {
				return iter.iterator().next();
			}
		} else {
			throw new ServerException("500");
		}
		return null;
	}

	public static void persistEntity(String kind, org.bson.Document entity, String type, MongoDatabase mongoDB) {
		MongoCollection<org.bson.Document> col = mongoDB.getCollection(kind);
		if (type.equals("INSERT"))
			col.insertOne(entity);
		else if (type.equals("UPDATE")) {
			org.bson.Document q = new org.bson.Document("id", entity.get("id"));
			col.updateOne(q, new org.bson.Document("$set", entity));
		}
	}

	public static void deleteEntity(String kind, org.bson.Document entity, MongoDatabase mongoDB) {
		MongoCollection<org.bson.Document> col = mongoDB.getCollection(kind);
		col.deleteOne(new org.bson.Document("id", new org.bson.Document("$eq", entity.get("id"))));
	}

	public static int exceptionHandler(Exception e) {
		if (e instanceof NumberFormatException || e instanceof DocumentException || e instanceof IOException
				|| e instanceof NullPointerException) {
			return HttpServletResponse.SC_BAD_REQUEST;
		} else if (e instanceof ServerException) {
			return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		} else if (e instanceof IllegalArgumentException) {
			if (e.getMessage().equals("409")) {
				return HttpServletResponse.SC_CONFLICT;
			} else {
				return HttpServletResponse.SC_BAD_REQUEST;
			}
		} else {
			return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		}
	}

	public static void setRespInitProps(HttpServletResponse resp) {
		resp.setContentType("application/xml");
		resp.setCharacterEncoding("UTF-8");
		resp.addHeader("Name-L3sid", "PrashantPardeshi 780");
	}
}
