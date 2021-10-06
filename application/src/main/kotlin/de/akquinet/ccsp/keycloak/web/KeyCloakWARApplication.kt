package de.akquinet.ccsp.keycloak.web

import org.keycloak.Config
import org.keycloak.exportimport.ExportImportManager
import org.keycloak.services.resources.KeycloakApplication
import org.keycloak.services.resources.RealmsResource
import org.keycloak.services.resources.WelcomeResource
import org.keycloak.services.resources.admin.AdminRoot
import org.keycloak.services.util.JsonConfigProviderFactory
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import javax.inject.Singleton

@Singleton // Needed, if beans.xml is present in deployment
class KeyCloakWARApplication : KeycloakApplication() {
  init {
    classes.add(WelcomeResource::class.java)
    classes.add(RealmsResource::class.java)
    classes.add(AdminRoot::class.java)
  }

  override fun bootstrap(): ExportImportManager {
    if (CONFIG_DIR != null) {
      // Add users, see importAddUser()
      copyAndFilterFileToConfigDirectory(USER_JSON)

      // See importRealms()
      val realmFile = copyAndFilterFileToConfigDirectory(REALM_JSON)

      if (realmFile != null) {
        // Will be used in super method
        System.setProperty("keycloak.import", realmFile)
      }
    } else {
      LOG.warn("WildFly config dir property not set: $CONFIG_PROPERTY")
    }

    return super.bootstrap()
  }

  override fun loadConfig() {
    val factory: JsonConfigProviderFactory = object : JsonConfigProviderFactory() {}
    Config.init(factory.create().orElseThrow { NoSuchElementException("No value present") })
  }

  /**
   * Every import/add method shows a different behaviour:
   *
   * - keycloak-server.json, e.g. may be located in META-INF/
   * - keycloak-add-user.json, MUST be located in ${jboss.server.config.dir}
   * - Import file of a realm may be located anywhere, but has to be defined in system property "keycloak.import"
   *
   * Source files may contain the following placeholders, which will be replaced by the according system property or a default value
   *
   * - @ENABLE_LDAP@ ("ubi.security.enable.ldap", defaults to "true")
   * - @ENABLE_DEV_USER@ ("ubi.security.enable.dev.user", defaults to "true")
   * - @ENABLE_SYNCH_USER@ ("ubi.security.enable.synch.user", defaults to "false")
   * - @ENABLE_MOBIL_USER@ ("ubi.security.enable.mobil.user", defaults to "false")
   *
   * Thus we have to copy the data from META-INF/ in the WAR to the expected location.
   *
   * @param fileName Name of the (JSON) file in config directory and META-INF, respectively
   * @return path to the copied file
   */
  private fun copyAndFilterFileToConfigDirectory(fileName: String): String? {
    val sourceStream = Thread.currentThread().contextClassLoader.getResourceAsStream("META-INF/$fileName")

    return if (sourceStream != null) {
      val content = sourceStream.bufferedReader().use { it.readText() }.replaceTemplates()
      val targetFile = File(CONFIG_DIR + File.separator + fileName)
      val targetStream = FileOutputStream(targetFile)

      targetStream.bufferedWriter().use { it.write(content) }
      targetFile.canonicalPath
    } else {
      LOG.warn("No source file found in JAR: $fileName")
      null
    }
  }

  enum class Template(val property: String, val default: String) {
    ENABLE_LDAP("ubi.security.enable.ldap", "true"),
    ENABLE_DEV_USER("ubi.security.enable.dev.user", "true"),
    ENABLE_SYNCH_USER("ubi.security.enable.synch.user", "false"),
    ENABLE_MOBIL_USER("ubi.security.enable.mobil.user", "false");
  }

  private fun String.replaceTemplates(): String {
    var result: String = this

    Template.values().forEach {
      val key = "@" + it.name + "@"
      val value = System.getProperty(it.property, it.default)

      result = result.replace(key, value)
    }

    return result
  }

  companion object {
    private val LOG = LoggerFactory.getLogger(KeyCloakWARApplication::class.java)

    const val USER_JSON = "keycloak-add-user.json"
    const val REALM_JSON = "keycloak-add-realm.json"
    const val CONFIG_PROPERTY = "jboss.server.config.dir"

    private val CONFIG_DIR = System.getProperty(CONFIG_PROPERTY)
  }
}