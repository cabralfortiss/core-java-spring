package eu.arrowhead.common.database.entity;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import eu.arrowhead.common.Defaults;

@Entity
@Table (uniqueConstraints = @UniqueConstraint(columnNames = {"operator", "name"}))
public class Cloud {
	
	//=================================================================================================
	// members
	
	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "updatedAt", "createdAt"); //NOSONAR
	
	@Id
	@GeneratedValue (strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column (nullable = false, length = Defaults.VARCHAR_BASIC)
	private String operator;
	
	@Column (nullable = false, length = Defaults.VARCHAR_BASIC)
	private String name;
	
	@Column (nullable = false)
	private boolean secure = false;
	
	@Column (nullable = false)
	private boolean neighbor = false;
	
	@Column (nullable = false)
	private boolean ownCloud = false;
	
	@Column (nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private ZonedDateTime createdAt;
	
	@Column (nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	private ZonedDateTime updatedAt;
	
	@OneToOne(mappedBy = "cloud", fetch = FetchType.EAGER, orphanRemoval = true, optional = true)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private CloudGatekeeper gatekeeper;
	
	@OneToMany (mappedBy = "cloud", fetch = FetchType.LAZY, orphanRemoval = true)
	@OnDelete (action = OnDeleteAction.CASCADE)
	private Set<AuthorizationInterCloud> authorizationInterClouds = new HashSet<>();
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Cloud() {}

	//-------------------------------------------------------------------------------------------------
	public Cloud(final String operator, final String name, final boolean secure, final boolean neighbor, final boolean ownCloud) {
		this.operator = operator;
		this.name = name;
		this.secure = secure;
		this.neighbor = neighbor;
		this.ownCloud = ownCloud;
	}
	
	//-------------------------------------------------------------------------------------------------
	@PrePersist
	public void onCreate() {
		this.createdAt = ZonedDateTime.now();
		this.updatedAt = this.createdAt;
	}
	
	//-------------------------------------------------------------------------------------------------
	@PreUpdate
	public void onUpdate() {
		this.updatedAt = ZonedDateTime.now();
	}

	//-------------------------------------------------------------------------------------------------
	public long getId() { return id; }
	public String getOperator() { return operator; }
	public String getName() { return name; }
	public boolean getSecure() { return secure; }
	public boolean getNeighbor() { return neighbor; }
	public boolean getOwnCloud() { return ownCloud; }
	public ZonedDateTime getCreatedAt() { return createdAt; }
	public ZonedDateTime getUpdatedAt() { return updatedAt; }
	public CloudGatekeeper getGatekeeper() { return gatekeeper; }
	public Set<AuthorizationInterCloud> getAuthorizationInterClouds() { return authorizationInterClouds; }

	//-------------------------------------------------------------------------------------------------
	public void setId(final long id) { this.id = id; }
	public void setOperator(final String operator) { this.operator = operator; }
	public void setName(final String name) { this.name = name; }
	public void setSecure(final boolean secure) { this.secure = secure; }
	public void setNeighbor(final boolean neighbor) { this.neighbor = neighbor; }
	public void setOwnCloud(final boolean ownCloud) { this.ownCloud = ownCloud; }
	public void setCreatedAt(final ZonedDateTime createdAt) { this.createdAt = createdAt; }
	public void setUpdatedAt(final ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }
	public void setGatekeeper(final CloudGatekeeper gatekeeper) { this.gatekeeper = gatekeeper; }
	public void setAuthorizationInterClouds(final Set<AuthorizationInterCloud> authorizationInterClouds) { this.authorizationInterClouds = authorizationInterClouds; }

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "Cloud [id = " + id + ", operator = " + operator + ", name = " + name + "]";
	}
}
