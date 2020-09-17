import React, { MouseEvent } from 'react';

import Grid from '@material-ui/core/Grid';
import './LandscapeDashboard.scss';
import { ILandscape, IItem, IAssessment, IGroup } from '../../../interfaces';
import { getAssessmentSummaryColorAndMessage } from '../LandscapeUtils/utils';

import Search from '../../SearchComponent/Search';

interface Props {
  landscape: ILandscape | null | undefined;
  assessments: IAssessment | undefined;
  onItemClick: (e: MouseEvent<HTMLSpanElement>, item: IItem) => void;
  onGroupClick: (e: MouseEvent<HTMLSpanElement>, group: IGroup) => void;
  onGroupAssessmentClick: (e: MouseEvent<HTMLSpanElement>, group: IGroup) => void;
  onItemAssessmentClick: (e: MouseEvent<HTMLSpanElement>, item: IItem) => void;
  findItem: (fullyQualifiedItemIdentifier: string) => void;
}

/**
 * Displays all groups of given landscape and provides all needed navigation
 */

const LandscapeDashboardLayout: React.FC<Props> = ({
  landscape,
  assessments,
  onItemClick,
  onGroupClick,
  onGroupAssessmentClick,
  onItemAssessmentClick,
  findItem,
}) => {
  // Render
  /*
    value         |0px     600px    960px    1280px   1920px
    key           |xs      sm       md       lg       xl
    screen width  |--------|--------|--------|--------|-------->
    range         |   xs   |   sm   |   md   |   lg   |   xl
     */

  const getItems = (group: IGroup) => {
    return group.items.map((item) => {
      const [assessmentColor, assessmentMessage] = getAssessmentSummaryColorAndMessage(
        assessments?.results[item.fullyQualifiedIdentifier],
        item.identifier
      );

      return (
        <Grid item key={item.identifier} className={'itemContainer'}>
          <span
            className='dot'
            id={item.fullyQualifiedIdentifier}
            style={{ backgroundColor: assessmentColor }}
          >
            <span
              className='statusDot'
              onClick={(e: MouseEvent<HTMLSpanElement>) => onItemAssessmentClick(e, item)}
            >
              <span className='statusField'>{assessmentMessage}</span>
            </span>
          </span>
          <div className='itemDescription'>
            <img src={item?.icon} className='icon' alt={'icon'} />
            <span
              className='itemName'
              onClick={(e: MouseEvent<HTMLSpanElement>) => onItemClick(e, item)}
            >
              {item.name || item.identifier}
            </span>
          </div>
        </Grid>
      );
    });
  };

  const getDashboardContent = () => {
    if (landscape && landscape.groups) {
      return landscape.groups.map((group) => {
        if (group.items.length > 0) {
          const groupColor = `#${group.color}` || 'grey';
          const [groupAssessmentColor] = getAssessmentSummaryColorAndMessage(
            assessments?.results[group.fullyQualifiedIdentifier],
            group.identifier
          );

          const items = getItems(group);

          return (
            <Grid item key={group.name} className='group' id={group.fullyQualifiedIdentifier}>
              <Grid item className='groupName' style={{ backgroundColor: groupColor }}>
                <span
                  className='groupLabel'
                  onClick={(e: MouseEvent<HTMLSpanElement>) => onGroupClick(e, group)}
                >
                  {group.name || group.identifier || ''}
                </span>
                <span
                  className='smallDot'
                  id={group.fullyQualifiedIdentifier}
                  style={{ backgroundColor: groupAssessmentColor }}
                  onClick={(e: MouseEvent<HTMLSpanElement>) => onGroupAssessmentClick(e, group)}
                ></span>
              </Grid>
              <Grid item className={'items'}>
                {items}
              </Grid>
            </Grid>
          );
        }
        return <React.Fragment key={group.identifier} />;
      });
    }
    return 'Loading landscapes...';
  };

  return (
    <div className='landscapeDashboardContainer' id='landscapeDashboardContainer'>
      <Grid className={'titleContainer'} container spacing={2}>
        <Grid item className='title' xs={12} md={7} lg={8} xl={10}>
          {landscape ? `${landscape.name}` : null}
        </Grid>
        <Grid item xs={12} md={5} lg={4} xl={2}>
          <Search findItem={findItem} />
        </Grid>
      </Grid>
      <Grid key={'group'} className={'groupContainer'} container spacing={5}>
        {getDashboardContent()}
      </Grid>
    </div>
  );
};

export default LandscapeDashboardLayout;
