module.exports = function (grunt) {
  grunt.loadNpmTasks('grunt-contrib-handlebars');
  grunt.loadNpmTasks('grunt-typescript');

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
    },
    typescript: {
      base: {
        src: ['js/**/*.ts'],
        dest: 'js',
        options: {
          module: 'amd', //or commonjs 
          target: 'es5', //or es3 
          basePath: 'js',
          sourceMap: true,
          declaration: true
        }
      }
    }
  });

  grunt.registerTask('default', ['handlebars', 'typescript']);
};
