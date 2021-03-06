package rql.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.core.JsonProcessingException;

import rql.RQLException;
import rql.Response;
import rql.impl.model.QueryField;

class SelectResponse implements Response {


	private Response response;
	private List<QueryField> fields;
	private boolean selectAll;

	public boolean isSelectAll() {
		return selectAll;
	}

	public void setSelectAll(boolean selectAll) {
		this.selectAll = selectAll;
	}

	private String resultAlias;

	public SelectResponse() {
	}

	public SelectResponse(Response resp) {
		this.response = resp;
	}

	public void setFields(List<QueryField> fields) {
		if (fields.size() == 0 || (fields.size() == 1 && fields.get(0).isAll()))
			this.selectAll = true;
		this.fields = fields;
	}

	public void addQueryField(QueryField field) {
		if (field.isAll())
			this.selectAll = true;
		if (this.fields == null)
			this.fields = new ArrayList<QueryField>();
		this.fields.add(field);
	}

	public List<QueryField> getFields() {
		return fields;
	}

	public String getResultAlias() {
		return resultAlias;
	}

	public void setResultAlias(String resultAlias) {
		this.resultAlias = resultAlias;
	}

	@Override
	public String getHeaderString(String name) {
		return response.getHeaderString(name);
	}

	@Override
	public MultivaluedMap<String, Object> getHeaders() {
		return response.getHeaders();
	}

	@Override
	public int getStatus() {
		return response.getStatus();
	}

	@Override
	public <T> List<T> getEntityAsList(Class<T> type) throws RQLException {
		List entity = response.getEntityAsList(HashMap.class);
		return (List<T>) entity.stream().map(i -> convertEntity(i, type)).collect(Collectors.toList());
	}

	private <T> T convertEntity(Object entity, Class<T> type) {
		if (type.isAssignableFrom(entity.getClass()))
			return (T) entity;

		// do object mapping

		if (type == String.class) {
			try {
				T result = (T) Utils.getObjectMapper().writeValueAsString(entity);
				return result;
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return Utils.getObjectMapper().convertValue(entity, type);
	}

	@Override
	public <T> T getEntity(Class<T> type) throws RQLException {
		if (selectAll) {
			return response.getEntity(type);
		}
		return convertEntity(getEntityAsMap(), type);
	}

	@Override
	public Map<String, Object> getEntityAsMap() throws RQLException {
		Map<String, Object> result = response.getEntityAsMap();
		if (!selectAll) {
			return fields.stream().filter(field -> {
				return result.containsKey(field.getField());
			}).map(field -> {
				return Pair.of(field.getAlias() != null ? field.getAlias() : field.getField(),
						result.get(field.getField()));
			}).collect(Collectors.toMap(e -> {
				return e.getKey();
			}, e -> {
				return e.getValue();
			}));
		}
		return result;
	}

	@Override
	public List<Map<String, Object>> getEntityAsListMap() throws RQLException {
		List<Map<String, Object>> result = response.getEntityAsListMap();
		if (!selectAll) {
			return result.stream().map(row -> {
				return fields.stream().filter(field -> {
					return row.containsKey(field.getField());
				}).map(field -> {
					return Pair.of(field.getAlias() != null ? field.getAlias() : field.getField(),
							row.get(field.getField()));
				}).collect(Collectors.toMap(e -> {
					return e.getKey();
				}, e -> {
					return e.getValue();
				}));
			}).collect(Collectors.toList());
		}
		return result;
	}

}
