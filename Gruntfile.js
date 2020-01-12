module.exports = function (grunt) {
  grunt.loadNpmTasks('grunt-contrib-handlebars');
  grunt.loadNpmTasks('grunt-ts');

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
          "public/templates.js": ["templates/**/*.hbs"]
        }
      }
    },
    ts: {
      default : {
        tsconfig: './tsconfig.json'
      }
    }
  });

  grunt.registerTask('default', ['handlebars', 'ts']);
};
