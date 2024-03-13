import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.12"

val appName = "personal-income-tax-submission-frontend"

lazy val coverageSettings: Seq[Setting[_]] = {

  val excludedPackages =
    "<empty>;Reverse.*;.*standardError*.*;.*govuk_wrapper*.*;.*main_template*.*;.*govuk_wrapper*.*;" +
      ".*main_template*.*;.*controllers.testonly.*;uk.gov.hmrc.BuildInfo;app.*;prod.*;config.*;models.*;" +
      ".*feedback*.*;partials.*;testOnly.*;testOnlyDoNotUseInAppConf.*;views.*;"

  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages,
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val twirlImports: Seq[String] = Seq(
  "config.AppConfig",
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._"
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(PlayKeys.playDefaultPort := 9308)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test ++ AppDependencies.itDependencies,
    TwirlKeys.templateImports ++= twirlImports,
    scalacOptions += "-Wconf:cat=unused-imports&src=html/.*:s",
    scalacOptions += "-Wconf:src=routes/.*:s"
  )
  .settings(Test / fork := false)
  .settings(
    DefaultBuildSettings.itSettings() ++
      Seq(unmanagedResourceDirectories.withRank(KeyRanks.Invisible) := Seq(baseDirectory.value / "it" / "resources")
  ))
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(coverageSettings: _*)
  .settings(
    // concatenate js
    Concat.groups := Seq(
      "javascripts/application.js" ->
        group(Seq(
          "lib/govuk-frontend/govuk/all.js",
          "javascripts/jquery.min.js",
          "javascripts/app.js",
          "javascripts/autocomplete.js"
        ))
    ),
    pipelineStages := Seq(digest),
    // below line required to force asset pipeline to operate in dev rather than only prod
    Assets / pipelineStages := Seq(concat)
  )

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())

addCommandAlias("runAllChecks", "clean;compile;scalastyle;coverage;test;it/test;coverageReport")
