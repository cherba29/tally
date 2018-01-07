module.exports = function (grunt) {

  grunt.loadNpmTasks('grunt-contrib-handlebars');

  // Project configuration.
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),

    handlebars: {
      options: {
        namespace: 'jbudget.templates',
        processName: function (filePath) {
          return filePath.replace(/^templates\//, '').replace(/\.hbs$/, '');
        },
        // Determine if preprocessed template functions will be wrapped in
        // Handlebars.template function.
        wrapped: true
      },
      all: {
        files: {
          "js/templates.js": ["templates/**/*.hbs"]
        }
      }
    }
  });

  grunt.registerTask('default', ['handlebars']);
};
