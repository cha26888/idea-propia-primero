(() => {
  for(const element of document.querySelectorAll('[data-shared-role="assistant"]')) {
    if(window.ChatFormatting)window.ChatFormatting.render(element,element.textContent);
  }
})();
