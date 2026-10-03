package com.viajesito.alert;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "alerts_log")
public class AlertLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long routeId;

	private Long priceHistoryId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AlertType alertType;

	@Column(length = 200)
	private String reasons;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(precision = 10, scale = 2)
	private BigDecimal referencePrice;

	@Column(precision = 6, scale = 2)
	private BigDecimal savingsPct;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AlertStatus status;

	@Column(columnDefinition = "text")
	private String message;

	@Column(length = 500)
	private String error;

	@Column(nullable = false)
	private OffsetDateTime sentAt;

	public Long getId() {
		return id;
	}

	public Long getRouteId() {
		return routeId;
	}

	public void setRouteId(Long routeId) {
		this.routeId = routeId;
	}

	public Long getPriceHistoryId() {
		return priceHistoryId;
	}

	public void setPriceHistoryId(Long priceHistoryId) {
		this.priceHistoryId = priceHistoryId;
	}

	public AlertType getAlertType() {
		return alertType;
	}

	public void setAlertType(AlertType alertType) {
		this.alertType = alertType;
	}

	public String getReasons() {
		return reasons;
	}

	public void setReasons(String reasons) {
		this.reasons = reasons;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public BigDecimal getReferencePrice() {
		return referencePrice;
	}

	public void setReferencePrice(BigDecimal referencePrice) {
		this.referencePrice = referencePrice;
	}

	public BigDecimal getSavingsPct() {
		return savingsPct;
	}

	public void setSavingsPct(BigDecimal savingsPct) {
		this.savingsPct = savingsPct;
	}

	public AlertStatus getStatus() {
		return status;
	}

	public void setStatus(AlertStatus status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public OffsetDateTime getSentAt() {
		return sentAt;
	}

	public void setSentAt(OffsetDateTime sentAt) {
		this.sentAt = sentAt;
	}

}
