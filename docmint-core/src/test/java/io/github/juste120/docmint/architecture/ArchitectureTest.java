package io.github.juste120.docmint.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/** Architecture rules of the constitution (Principle V), enforced on every build. */
class ArchitectureTest {

  private static final String ROOT = "io.github.juste120.docmint";
  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(ROOT);

  private static final String[] THIRD_PARTY_ENGINES = {
    "io.pebbletemplates..",
    "com.openhtmltopdf..",
    "org.apache.pdfbox..",
    "org.jsoup..",
    "com.google.zxing.."
  };

  @Test
  void domainDependsOnNothing() {
    classes()
        .that()
        .resideInAPackage(ROOT + ".domain..")
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(ROOT + ".domain..", "java..")
        .check(CLASSES);
  }

  @Test
  void portsDependOnlyOnTheDomain() {
    classes()
        .that()
        .resideInAPackage(ROOT + ".port..")
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(ROOT + ".port..", ROOT + ".domain..", "java..")
        .check(CLASSES);
  }

  @Test
  void applicationKnowsOnlyPortsDomainAndApiContracts() {
    classes()
        .that()
        .resideInAPackage(ROOT + ".application..")
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(
            ROOT + ".application..",
            ROOT + ".port..",
            ROOT + ".domain..",
            ROOT + ".api..",
            "java..",
            "org.slf4j..")
        .check(CLASSES);
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".application..")
        .should()
        .dependOnClassesThat()
        .haveFullyQualifiedName(ROOT + ".api.DocMint")
        .orShould()
        .dependOnClassesThat()
        .haveFullyQualifiedName(ROOT + ".api.DocMintBuilder")
        .check(CLASSES);
  }

  @Test
  void onlyInfrastructureUsesThirdPartyEngines() {
    noClasses()
        .that()
        .resideOutsideOfPackage(ROOT + ".infrastructure..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(THIRD_PARTY_ENGINES)
        .check(CLASSES);
  }

  @Test
  void infrastructureDoesNotUseTheApplicationLayerOrTheFacade() {
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".infrastructure..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(ROOT + ".application..")
        .orShould()
        .dependOnClassesThat()
        .haveFullyQualifiedName(ROOT + ".api.DocMint")
        .orShould()
        .dependOnClassesThat()
        .haveFullyQualifiedName(ROOT + ".api.DocMintBuilder")
        .check(CLASSES);
  }

  @Test
  void coreNeverDependsOnSpring() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.springframework..")
        .check(CLASSES);
  }

  @Test
  void publicApiDoesNotExposeInfrastructureTypes() {
    noClasses()
        .that()
        .resideInAnyPackage(ROOT + ".api..", ROOT + ".port..", ROOT + ".domain..")
        .and()
        .doNotHaveFullyQualifiedName(ROOT + ".api.DocMintBuilder")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(ROOT + ".infrastructure..")
        .check(CLASSES);
  }
}
