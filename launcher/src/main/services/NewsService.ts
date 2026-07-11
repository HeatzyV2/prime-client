import { BUNDLED_NEWS } from '../../shared/ecosystem-catalog'
import type { NewsItem } from '../../shared/types'

export class NewsService {
  getNews(): NewsItem[] {
    return BUNDLED_NEWS
  }
}

export const newsService = new NewsService()
