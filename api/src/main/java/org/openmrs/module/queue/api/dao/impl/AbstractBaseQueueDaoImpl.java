/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api.dao.impl;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.NotNull;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.Auditable;
import org.openmrs.OpenmrsObject;
import org.openmrs.Retireable;
import org.openmrs.Voidable;
import org.openmrs.api.db.hibernate.HibernateUtil;
import org.openmrs.module.queue.api.dao.BaseQueueDao;

@Slf4j
@Getter
@Setter(AccessLevel.MODULE)
@SuppressWarnings("unchecked")
public class AbstractBaseQueueDaoImpl<Q extends OpenmrsObject & Auditable> implements BaseQueueDao<Q> {
	
	private final SessionFactory sessionFactory;
	
	private final Class<Q> clazz;
	
	public AbstractBaseQueueDaoImpl(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
		this.clazz = (Class<Q>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}
	
	protected Session getCurrentSession() {
		return this.getSessionFactory().getCurrentSession();
	}
	
	@Override
	public Optional<Q> get(int id) {
		return Optional.ofNullable(getCurrentSession().get(this.clazz, id));
	}
	
	@Override
	public Optional<Q> get(@NotNull String uuid) {
		CriteriaBuilder cb = getCurrentSession().getCriteriaBuilder();
		CriteriaQuery<Q> query = cb.createQuery(getClazz());
		Root<Q> root = query.from(getClazz());
		List<Predicate> predicates = new ArrayList<>();
		includeVoidedObjects(cb, predicates, root, false);
		predicates.add(cb.equal(root.get("uuid"), uuid));
		query.where(predicates.toArray(new Predicate[0]));
		return getCurrentSession().createQuery(query).uniqueResultOptional();
	}
	
	@Override
	public Q createOrUpdate(Q queue) {
		return HibernateUtil.saveOrUpdate(this.getCurrentSession(), queue);
	}
	
	@Override
	public void delete(Q queue) {
		this.getCurrentSession().remove(queue);
	}
	
	@Override
	public void delete(@NotNull String uuid) {
		this.get(uuid).ifPresent(this::delete);
	}
	
	@Override
	public List<Q> findAll() {
		return this.findAll(false);
	}
	
	@Override
	public List<Q> findAll(boolean includeVoided) {
		CriteriaBuilder cb = getCurrentSession().getCriteriaBuilder();
		CriteriaQuery<Q> query = cb.createQuery(clazz);
		Root<Q> root = query.from(clazz);
		List<Predicate> predicates = new ArrayList<>();
		includeVoidedObjects(cb, predicates, root, includeVoided);
		query.where(predicates.toArray(new Predicate[0]));
		return getCurrentSession().createQuery(query).list();
	}
	
	protected boolean isVoidable() {
		return Voidable.class.isAssignableFrom(clazz);
	}
	
	protected boolean isRetireable() {
		return Retireable.class.isAssignableFrom(clazz);
	}
	
	protected void handleVoidable(CriteriaBuilder cb, List<Predicate> predicates, Root<Q> root) {
		predicates.add(cb.equal(root.get("voided"), false));
	}
	
	protected void handleRetireable(CriteriaBuilder cb, List<Predicate> predicates, Root<Q> root) {
		predicates.add(cb.equal(root.get("retired"), false));
	}
	
	protected void includeVoidedObjects(CriteriaBuilder cb, List<Predicate> predicates, Root<Q> root,
	        boolean includeRetired) {
		if (!includeRetired) {
			if (isVoidable()) {
				handleVoidable(cb, predicates, root);
			} else if (isRetireable()) {
				handleRetireable(cb, predicates, root);
			}
		}
	}
	
	/**
	 * If the passed value is null, return without limiting If the passed value is not null, add clause
	 * that the property must be equal to the value
	 */
	protected void limitToEqualsProperty(CriteriaBuilder cb, List<Predicate> predicates, Path<?> property, Object value) {
		if (value != null) {
			predicates.add(cb.equal(property, value));
		}
	}
	
	/**
	 * If the passed value is null, return without limiting If the passed value is not null, add clause
	 * that the property must greater or equal to the value
	 */
	protected <Y extends Comparable<? super Y>> void limitToGreaterThanOrEqualToProperty(CriteriaBuilder cb,
	        List<Predicate> predicates, Path<Y> property, Y value) {
		if (value != null) {
			predicates.add(cb.greaterThanOrEqualTo(property, value));
		}
	}
	
	/**
	 * If the passed value is null, return without limiting If the passed value is not null, add clause
	 * that the property must be less or equal to the value
	 */
	protected <Y extends Comparable<? super Y>> void limitToLessThanOrEqualToProperty(CriteriaBuilder cb,
	        List<Predicate> predicates, Path<Y> property, Y value) {
		if (value != null) {
			predicates.add(cb.lessThanOrEqualTo(property, value));
		}
	}
	
	/**
	 * If the passed values is null, return without limiting If the passed values is empty, add clause
	 * that the property must be null If the passed values is not empty, add clause that the property
	 * must be one of the given values
	 */
	protected void limitByCollectionProperty(List<Predicate> predicates, Path<?> property, Collection<?> values) {
		if (values != null) {
			if (values.isEmpty()) {
				predicates.add(property.isNull());
			} else {
				predicates.add(property.in(values));
			}
		}
	}
}
