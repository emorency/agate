/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

agate.ticket

  .controller('TicketListController', ['$scope', 'TicketsResource',

    function ($scope, TicketsResource) {

      $scope.tickets = TicketsResource.get();

      $scope.deleteTicket = function (id) {
        //TODO ask confirmation
        TicketResource.delete({id: id},
          function () {
            $scope.tickets = TicketsResource.get();
          });
      };

    }]);
