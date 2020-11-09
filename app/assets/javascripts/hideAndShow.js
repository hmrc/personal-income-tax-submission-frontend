(function() {
    var Radios = window.GOVUKFrontend.Radios
    var $radio = document.querySelector('[data-module="govuk-radios"]')
    if ($radio) {
        new Radios($radio).init()
    }
})();
